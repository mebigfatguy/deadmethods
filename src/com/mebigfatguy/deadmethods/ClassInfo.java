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
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;

public class ClassInfo {
	private final String className;
	private final String superClassName;
	private final String[] interfaces;
	private final int classAccess;
	private final Set<MethodInfo> methodInfo;
	private final Set<ClassInfo> derivedClasses;

	public ClassInfo(String name, String superName, String[] infs, int access) {
		className = name;
		superClassName = superName;
		interfaces = infs;
		classAccess = access;
		methodInfo = new HashSet<MethodInfo>();
		derivedClasses = new HashSet<ClassInfo>();
	}

	public MethodInfo addMethod(String name, String signature, int access) {
		MethodInfo minfo = new MethodInfo(name, signature, access);
		methodInfo.add(minfo);
		return minfo;
	}

	public String getClassName() {
		return className;
	}

	public String getPackageName() {
		int slashPos = className.lastIndexOf('/');
		if (slashPos < 0) {
			return "";
		}

		return className.substring(0, slashPos).replaceAll("/", ".");
	}

	public String getSuperClassName() {
		return superClassName;
	}

	public String[] getInterfaceNames() {
		return interfaces;
	}

	public boolean isInterface() {
		return (classAccess & Opcodes.ACC_INTERFACE) != 0;
	}

	public boolean isSynthetic() {
		return (classAccess & Opcodes.ACC_SYNTHETIC) != 0;
	}
	
	public boolean isAnnotation() {
	    return (classAccess & Opcodes.ACC_ANNOTATION) != 0;
	}

	public int getAccess() {
		return classAccess;
	}

	public Set<MethodInfo> getMethodInfo() {
		return methodInfo;
	}

	public void addDerivedClass(ClassInfo derived) {
		derivedClasses.add(derived);
	}

	public Set<ClassInfo> getDerivedClasses() {
		return Collections.<ClassInfo>unmodifiableSet(derivedClasses);
	}

	@Override
	public String toString() {
		return className;
	}
}
