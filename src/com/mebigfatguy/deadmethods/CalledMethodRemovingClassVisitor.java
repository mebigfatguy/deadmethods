package com.mebigfatguy.deadmethods;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;


class CalledMethodRemovingClassVisitor implements ClassVisitor {
	
    private final CalledMethodRemovingMethodVisitor m_calledMethodRemovingMethodVisitor;

    public CalledMethodRemovingClassVisitor(FindDeadMethods findDeadMethods) {
        m_calledMethodRemovingMethodVisitor = new CalledMethodRemovingMethodVisitor(findDeadMethods);
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
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
        return m_calledMethodRemovingMethodVisitor;
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String desc) {
    }

    @Override
    public void visitSource(final String source, final String debug) {
    }
}