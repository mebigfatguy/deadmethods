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

import org.objectweb.asm.Opcodes;

public class MethodInfo {

	private final String methodName;
	private final String methodSignature;
	private final int methodAccess;
	private boolean isTest;

	public MethodInfo(String name, String signature, int access) {
		methodName = name;
		methodSignature = signature;
		methodAccess = access;
		isTest = false;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getMethodSignature() {
		return methodSignature;
	}

	public int getMethodAccess() {
		return methodAccess;
	}

	public boolean isTest() {
		return isTest;
	}

	public void setTest(boolean test) {
		isTest = test;
	}

	@Override
	public int hashCode() {
		return methodName.hashCode() ^ methodSignature.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof MethodInfo) {
			MethodInfo that = (MethodInfo) o;
			return methodName.equals(that.methodName)
			    && methodSignature.equals(that.methodSignature)
			  && ((methodAccess & Opcodes.ACC_STATIC) == (that.methodAccess & Opcodes.ACC_STATIC));
		}
		return false;
	}
	@Override
	public String toString() {
		return methodName + methodSignature;
	}
}
