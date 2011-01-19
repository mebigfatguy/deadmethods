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

import java.util.HashSet;
import java.util.Set;

public class ClassInfo {
	private final String className;
	private final int classAccess;
	private final Set<MethodInfo> methodInfo;

	public ClassInfo(String name, int access) {
		className = name;
		classAccess = access;
		methodInfo = new HashSet<MethodInfo>();
	}

	public void addMethod(String name, String signature, int access) {
		methodInfo.add(new MethodInfo(name, signature, access));
	}

	public String getClassName() {
		return className;
	}

	public int getAccess() {
		return classAccess;
	}

	public Set<MethodInfo> getMethodInfo() {
		return methodInfo;
	}


}
