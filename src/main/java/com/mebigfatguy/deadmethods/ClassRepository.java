/*
 * deadmethods - A unused methods detector
 * Copyright 2011-2018 MeBigFatGuy.com
 * Copyright 2011-2018 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.deadmethods;

import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClassRepository implements Iterable<String> {

    private final ClassPath path;
    private final ProgressLogger logger;
    private final ClassLoader loader;
    private final Map<String, ClassInfo> classInfo;
    private final AtomicBoolean scanning;
    private ExecutorService executor;

    public ClassRepository(ClassPath path2, ClassPath auxPath, ProgressLogger progressLogger) {
        path = path2;
        logger = progressLogger;
        loader = createClassLoader(path2, auxPath);
        classInfo = new ConcurrentHashMap<>();
        scanning = new AtomicBoolean();
    }

    public void startScanning() {
        if (scanning.getAndSet(true)) {
            return;
        }

        try {
            getClassInfo("java/lang/Object");
        } catch (IOException e) {
        }
        
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        executor.execute(new ClassPopulator());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }

    public void terminate() {
        if (!scanning.getAndSet(true)) {
            return;
        }

        executor.shutdown();
    }

    public ClassInfo getClassInfo(String clsName) throws IOException {
        if (clsName == null) {
            return null;
        }

        ClassInfo info = classInfo.get(clsName);
        if ((info == null) && !clsName.startsWith("[")) {
            info = loadClassIntoRepository(clsName);
        }
        return info;
    }

    public Collection<ClassInfo> getAllClassInfos() {
        return Collections.unmodifiableCollection(classInfo.values());
    }

    public Set<MethodInfo> getMethodInfo(String clsName) throws IOException {
        ClassInfo info = classInfo.get(clsName);
        if (info == null) {
            info = loadClassIntoRepository(clsName);
        }

        return Collections.unmodifiableSet(info.getMethodInfo());
    }

    @Override
    public Iterator<String> iterator() {
        return new PathIterator(path, ".class", logger);
    }

    public Iterator<String> xmlIterator() {
        return new PathIterator(path, ".xml", logger);
    }

    public Iterator<String> serviceIterator() {
        return new PathPrefixIterator(path, "/META-INF/services", logger);
    }

    private final ClassLoader createClassLoader(final ClassPath classpath, final ClassPath auxClassPath) {
        return AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            @Override
            public URLClassLoader run() {
                Set<URL> urls = new HashSet<>();

                urls.addAll(convertPathToURLs(classpath));
                urls.addAll(convertPathToURLs(auxClassPath));

                return new URLClassLoader(urls.toArray(new URL[urls.size()]));
            }
        });
    }

    private List<URL> convertPathToURLs(ClassPath clsPath) {
        List<URL> urls = new ArrayList<>();

        Iterator<String> it = clsPath.iterator();
        while (it.hasNext()) {
            try {
                String resource = it.next();
                File file = new File(resource);
                if (file.exists()) {
                    if (file.getAbsolutePath().endsWith(".jar")) {
                        urls.add(new URL("jar", "", "file://" + file.getAbsolutePath() + "!/"));
                    } else {
                        urls.add(file.toURI().toURL());
                    }
                } else {
                    logger.log("ClassPath root does not exist: " + file.getAbsolutePath());
                }
            } catch (MalformedURLException murle) {
                // do something
            }
        }

        return urls;
    }

    public InputStream getClassStream(String clsName) {
        return loader.getResourceAsStream(clsName + ".class");
    }

    public InputStream getStream(String xmlName) {
        return loader.getResourceAsStream(xmlName);
    }

    private ClassInfo loadClassIntoRepository(String clsName) throws IOException {
        try (InputStream is = getClassStream(clsName)) {
            ClassReader cr = new ClassReader(is);
            ClassRepositoryVisitor crv = new ClassRepositoryVisitor();
            cr.accept(crv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
            ClassInfo info = crv.getClassInfo();
            ClassInfo existing = classInfo.putIfAbsent(clsName, info);
            if (existing != null) {
                info = existing;
            }

            if (!"java/lang/Object".equals(clsName)) {
                String superClassName = info.getSuperClassName();
                ClassInfo superInfo = getClassInfo(superClassName);
                if (superInfo != null) {
                    superInfo.addDerivedClass(info);
                }

                String[] interfaceNames = info.getInterfaceNames();
                for (String interfaceName : interfaceNames) {
                    ClassInfo infInfo = getClassInfo(interfaceName);
                    infInfo.addDerivedClass(info);
                }
            }

            return info;
        } catch (Exception e) {
            logger.log("Failed opening class into repository: " + clsName);
            throw new IOException("Failed opening class into repository: " + clsName, e);
        }
    }

    @Override
    public String toString() {
        return "ClassRepository[path = " + path + ", classInfo = " + classInfo + "]";
    }

    class ClassPopulator implements Runnable {

        @Override
        public void run() {
            Iterator<String> it = iterator();
            while (it.hasNext()) {
                String className = it.next();
                if (!className.startsWith("[")) {
                    executor.execute(new ClassScanner(className));
                }
            }
        }
    }

    class ClassScanner implements Runnable {

        private String clsName;

        public ClassScanner(String className) {
            clsName = className;
        }

        public void run() {
            try {
                getClassInfo(clsName);
            } catch (IOException e) {
            }
        }
    }
}
