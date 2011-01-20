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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class CalledMethodCollectingVisitor implements ClassVisitor {

	private final FindDeadMethods findDeadMethods;

	/**
	 * @param findDeadMethods
	 */
	CalledMethodCollectingVisitor(FindDeadMethods findDeadMethods) {
		this.findDeadMethods = findDeadMethods;
	}

	private String m_clsName;
    private boolean m_skip;

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        m_clsName = name;

        addHierarchy(superName, name);
        for (String inf : interfaces) {
            addHierarchy(inf, name);
        }

        m_skip = (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    private void addHierarchy(final String superName, final String subName) {
//        if (!superName.equals(subName)) {
//            Set<String> children = this.findDeadMethods.hierarchy.get(superName);
//            if (children == null) {
//                children = new HashSet<String>();
//                this.findDeadMethods.hierarchy.put(superName, children);
//            }
//            children.add(subName);
//        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return null;
    }

    @Override
    public void visitAttribute(final Attribute attr) {
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
        return null;
    }

    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
//        if (!name.equals("<clinit>")) {
//            if (!m_skip) {
//                if ((access & Opcodes.ACC_SYNTHETIC) == 0) {
//                    this.findDeadMethods.methodNames.add(m_clsName + ":" + name + desc);
//                }
//            }
//        }
        return null;
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String desc) {
    }

    @Override
    public void visitSource(final String source, final String debug) {
    }
}