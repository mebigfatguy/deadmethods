/*
 * deadmethods - A unused methods detector
 * Copyright 2011 MeBigFatGuy.com
 * Copyright 2011 Dave Brosius
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
import org.apache.tools.ant.types.resources.FileResource;
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
		ClassInfo info = classInfo.get(clsName);
		if (info == null) {
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
		return new PathIterator(path);
	}

	private final ClassLoader createClassLoader(final Path classpath, final Path auxClassPath) {
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

	private List<URL> convertPathToURLs(Path path) {
		List<URL> urls = new ArrayList<URL>();

		@SuppressWarnings("unchecked")
		Iterator<FileResource> it = path.iterator();
		while (it.hasNext()) {
			try {
				FileResource resource = it.next();
				File file = resource.getFile();
				if (file.getAbsolutePath().endsWith(".jar")) {
					urls.add(new URL("jar", "", "file://" + file.getAbsolutePath() + "!/"));
				} else {
					urls.add(file.toURI().toURL());
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
				superInfo.addDerivedClass(info);

				String[] interfaceNames = info.getInterfaceNames();
				for (String interfaceName : interfaceNames) {
					ClassInfo infInfo = getClassInfo(interfaceName);
					infInfo.addDerivedClass(info);
				}
			}

			return info;
		} finally {
			Closer.close(is);
		}
	}
}
