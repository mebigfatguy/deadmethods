/*
 * deadmethods - A unused methods detector
 * Copyright 2011-2012 MeBigFatGuy.com
 * Copyright 2011-2012 Dave Brosius
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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
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

	@Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return null;
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return null;
    }

    @Override
    public void visitAttribute(final Attribute attr) {
    }

    @Override
    public void visitCode() {
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
    }

    @Override
    public void visitFrame(final int type, final int nLocal, final Object[] local, final int nStack, final Object[] stack) {
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
    }

    @Override
    public void visitInsn(final int opcode) {
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
    }

    @Override
    public void visitLabel(final Label label) {
    }

    @Override
    public void visitLdcInsn(final Object cst) {
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
    }

    @Override
    public void visitLocalVariable(final String name, final String desc, final String signature, final Label start, final Label end, final int index) {
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
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

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
        return null;
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
    }

    @Override
    public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
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
