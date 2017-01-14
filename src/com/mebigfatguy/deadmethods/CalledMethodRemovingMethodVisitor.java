/*
 * deadmethods - A unused methods detector
 * Copyright 2011-2017 MeBigFatGuy.com
 * Copyright 2011-2017 Dave Brosius
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
import java.lang.reflect.Constructor;
import java.util.Set;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CalledMethodRemovingMethodVisitor extends MethodVisitor {

    enum State {
        NONE, NEW_TOS
    };

    private final ClassRepository repo;
    private final Set<String> methods;
    private State state;

    public CalledMethodRemovingMethodVisitor(ClassRepository repository, Set<String> allMethods) {
        super(Opcodes.ASM5);
        repo = repository;
        methods = allMethods;
        state = State.NONE;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        String methodInfo = owner + ":" + name + desc;
        methods.remove(methodInfo);

        try {
            if (!owner.startsWith("[")) {
                ClassInfo info = repo.getClassInfo(owner);
                clearDerivedMethods(info, name + desc);
                clearInheritedMethods(info, name + desc);
            }

            processReflection(opcode, owner, name);
        } catch (IOException ioe) {
        }
    }

    @Override
    public void visitInsn(int opcode) {
        state = State.NONE;
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        state = State.NONE;
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        state = State.NONE;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if ((opcode == Opcodes.CHECKCAST) && (state == State.NEW_TOS)) {
            try {
                ClassInfo rootInfo = repo.getClassInfo(type);
                if (rootInfo != null) {
                    clearConstructors(rootInfo);
                }
            } catch (IOException ioe) {
            }
        }
        state = State.NONE;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        state = State.NONE;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        state = State.NONE;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        state = State.NONE;
    }

    @Override
    public void visitLdcInsn(Object cst) {
        state = State.NONE;
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        state = State.NONE;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        state = State.NONE;
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        state = State.NONE;
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        state = State.NONE;
    }

    private void processReflection(int opcode, String owner, String name) {
        if (opcode != Opcodes.INVOKEVIRTUAL) {
            state = State.NONE;
            return;
        }

        if ("newInstance".equals(name) && owner.equals(Constructor.class.getName().replaceAll("\\.", "/"))) {
            state = State.NEW_TOS;
        }
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
    }

    private void clearDerivedMethods(ClassInfo info, String methodInfo) {
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
            while (infInfo != null) {
                methods.remove(infInfo.getClassName() + ":" + methodInfo);
                clearInheritedMethods(infInfo, methodInfo);
                infInfo = repo.getClassInfo(infInfo.getSuperClassName());
            }
        }
    }

    private void clearConstructors(ClassInfo info) {

        for (MethodInfo methodInfo : info.getMethodInfo()) {
            if ("<init>".equals(methodInfo.getMethodName())) {
                methods.remove(info.getClassName() + ":" + methodInfo);
            }
        }

        Set<ClassInfo> derivedInfos = info.getDerivedClasses();
        for (ClassInfo derivedInfo : derivedInfos) {
            clearConstructors(derivedInfo);
        }
    }
}
