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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.types.Path;
import org.objectweb.asm.ClassReader;

import com.sun.xml.internal.ws.org.objectweb.asm.Opcodes;

public class ClassRepository {

	private final ClassLoader loader;
	private final Map<String, ClassInfo> classInfo;

	public ClassRepository(Path classpath) {
		loader = createClassLoader(classpath);
		classInfo = new HashMap<String, ClassInfo>();
	}

	public boolean isInterface(String clsName) throws IOException {
		ClassInfo info = classInfo.get(clsName);
		if (info == null) {
			info = loadClassIntoRepository(clsName);
		}

		return (info.getAccess() & Opcodes.ACC_INTERFACE) != 0;
	}

	public Set<MethodInfo> getMethodInfo(String clsName) throws IOException {
		ClassInfo info = classInfo.get(clsName);
		if (info == null) {
			info = loadClassIntoRepository(clsName);
		}

		return Collections.<MethodInfo>unmodifiableSet(info.getMethodInfo());
	}

	public final ClassLoader createClassLoader(final Path classpath) {
		return AccessController.<URLClassLoader>doPrivileged(new PrivilegedAction<URLClassLoader>() {
			@Override
			public URLClassLoader run() {
				List<URL> urls = new ArrayList<URL>();
				@SuppressWarnings("unchecked")
				Iterator<String> it = classpath.iterator();
				while (it.hasNext()) {
					try {
						String path = it.next();
						if (path.endsWith(".jar")) {
							urls.add(new URL("jar", "", "file://" + path + "!/"));
						} else {
							urls.add(new URL("file://" + path));
						}
					} catch (MalformedURLException murle) {
						//do something
					}
				}
				return new URLClassLoader(urls.toArray(new URL[urls.size()]));
			}
		});
	}

	private ClassInfo loadClassIntoRepository(String clsName) throws IOException {
		InputStream is = null;
		try {
			is = loader.getResourceAsStream("/" + clsName + ".class");
			ClassReader cr = new ClassReader(is);
			ClassRepositoryVisitor crv = new ClassRepositoryVisitor();
			cr.accept(crv, ClassReader.SKIP_DEBUG|ClassReader.SKIP_CODE);
			ClassInfo info = crv.getClassInfo();
			classInfo.put(clsName, info);
			return info;
		} finally {
			Closer.close(is);
		}
	}
}
