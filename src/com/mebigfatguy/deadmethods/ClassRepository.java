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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.types.Path;

import com.sun.xml.internal.ws.org.objectweb.asm.Opcodes;

public class ClassRepository {

	Map<String, ClassInfo> classInfo = new HashMap<String, ClassInfo>();

	public ClassRepository(Path classpath) {

	}

	public boolean isInterface(String clsName) {
		ClassInfo info = classInfo.get(clsName);
		if (info == null) {
			info = loadClassIntoRepository(clsName);
		}

		return (info.getAccess() & Opcodes.ACC_INTERFACE) != 0;
	}

	public Set<MethodInfo> getMethodInfo(String clsName) {
		ClassInfo info = classInfo.get(clsName);
		if (info == null) {
			info = loadClassIntoRepository(clsName);
		}

		return Collections.<MethodInfo>unmodifiableSet(info.getMethodInfo());
	}

	private ClassInfo loadClassIntoRepository(String clsName) {
		return null;
	}
}
