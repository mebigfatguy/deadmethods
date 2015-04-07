/*
 * deadmethods - A unused methods detector
 * Copyright 2011-2015 MeBigFatGuy.com
 * Copyright 2011-2015 Dave Brosius
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.objectweb.asm.ClassReader;

public class ClassRepository implements Iterable<String> {

	private final Path path;
	private final ClassLoader loader;
	private final Map<String, ClassInfo> classInfo;

	public ClassRepository(Path classpath, Path auxClassPath) {
		path = classpath;
		loader = createClassLoader(classpath, auxClassPath);
		classInfo = new HashMap<String, ClassInfo>();
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
		return Collections.<ClassInfo>unmodifiableCollection(classInfo.values());
	}

	public Set<MethodInfo> getMethodInfo(String clsName) throws IOException {
		ClassInfo info = classInfo.get(clsName);
		if (info == null) {
			info = loadClassIntoRepository(clsName);
		}

		return Collections.<MethodInfo>unmodifiableSet(info.getMethodInfo());
	}

	@Override
	public Iterator<String> iterator() {
		return new PathIterator(path, ".class");
	}
	
	public Iterator<String> xmlIterator() {
	    return new PathIterator(path, ".xml");
	}
	
	public Iterator<String> serviceIterator() {
	    return new PathPrefixIterator(path, "/META-INF/services");
	}

	private static final ClassLoader createClassLoader(final Path classpath, final Path auxClassPath) {
		return AccessController.<URLClassLoader>doPrivileged(new PrivilegedAction<URLClassLoader>() {
			@Override
			public URLClassLoader run() {
				Set<URL> urls = new HashSet<URL>();

				urls.addAll(convertPathToURLs(classpath));
				urls.addAll(convertPathToURLs(auxClassPath));

				return new URLClassLoader(urls.toArray(new URL[urls.size()]));
			}
		});
	}

	private static List<URL> convertPathToURLs(ResourceCollection clsPath) {
		List<URL> urls = new ArrayList<URL>();

		Iterator<Resource> it = clsPath.iterator();
		while (it.hasNext()) {
			try {
				Resource resource = it.next();
				File file = new File(resource.toString());
				if (file.exists()) {
					if (file.getAbsolutePath().endsWith(".jar")) {
						urls.add(new URL("jar", "", "file://" + file.getAbsolutePath() + "!/"));
					} else {
						urls.add(file.toURI().toURL());
					}
				} else {
					TaskFactory.getTask().log("ClassPath root does not exist: " + file.getAbsolutePath());
				}
			} catch (MalformedURLException murle) {
				//do something
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
		InputStream is = null;
		try {
			is = getClassStream(clsName);
			ClassReader cr = new ClassReader(is);
			ClassRepositoryVisitor crv = new ClassRepositoryVisitor();
			cr.accept(crv, ClassReader.SKIP_DEBUG|ClassReader.SKIP_CODE);
			ClassInfo info = crv.getClassInfo();
			classInfo.put(clsName, info);

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
		} catch (IOException ioe) {
			TaskFactory.getTask().log("Failed opening class into repository: " + clsName);
			throw new IOException("Failed opening class into repository: " + clsName, ioe);
		} finally {
			Closer.close(is);
		}
	}
}
