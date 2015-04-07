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

import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


class CalledMethodRemovingClassVisitor extends ClassVisitor {

    private final CalledMethodRemovingMethodVisitor m_calledMethodRemovingMethodVisitor;

    public CalledMethodRemovingClassVisitor(ClassRepository repository, Set<String> methods) {
        super(Opcodes.ASM4);
        m_calledMethodRemovingMethodVisitor = new CalledMethodRemovingMethodVisitor(repository, methods);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        return m_calledMethodRemovingMethodVisitor;
    }
}