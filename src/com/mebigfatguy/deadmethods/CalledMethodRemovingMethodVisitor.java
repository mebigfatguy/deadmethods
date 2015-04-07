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

import java.io.IOException;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CalledMethodRemovingMethodVisitor extends MethodVisitor {

	private final ClassRepository repo;
	private final Set<String> methods;

	public CalledMethodRemovingMethodVisitor(ClassRepository repository, Set<String> allMethods) {
	    super(Opcodes.ASM4);
		repo = repository;
		methods = allMethods;
	}
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    	String methodInfo = owner + ":" + name + desc;
    	methods.remove(methodInfo);

    	try {
    		if (!owner.startsWith("[")) {
	    		ClassInfo info = repo.getClassInfo(owner);
	    		clearDerivedMethods(info, name + desc);
				clearInheritedMethods(info, name + desc);
    		}
    	} catch (IOException ioe) {
    	}
    }

    private void clearDerivedMethods(ClassInfo info, String methodInfo) throws IOException {
    	Set<ClassInfo> derivedInfos = info.getDerivedClasses();

    	for (ClassInfo derivedInfo : derivedInfos) {
    		methods.remove(derivedInfo.getClassName() + ":" + methodInfo);
    		clearDerivedMethods(derivedInfo, methodInfo);
    	}
    }

    private void clearInheritedMethods(ClassInfo info, String methodInfo) throws IOException {
    	ClassInfo superInfo = repo.getClassInfo(info.getSuperClassName());
        while (superInfo != null) {
    		methods.remove(superInfo.getClassName() + ":" + methodInfo);
    		clearInheritedMethods(superInfo, methodInfo);
			superInfo = repo.getClassInfo(superInfo.getSuperClassName());
    	}

        for (String interfaceName : info.getInterfaceNames()) {
            ClassInfo infInfo = repo.getClassInfo(interfaceName);
            methods.remove(infInfo.getClassName() + ":" + methodInfo);
            clearInheritedMethods(infInfo, methodInfo);
            infInfo = repo.getClassInfo(infInfo.getSuperClassName());
        }
    }
}
