package com.mebigfatguy.deadmethods;

import org.objectweb.asm.AnnotationVisitor;


class MethodCollectingAnnotationVisitor implements AnnotationVisitor {
    private final Skipper m_skipper;
    private boolean m_skip;

    public MethodCollectingAnnotationVisitor(final Skipper skipper) {
        m_skipper = skipper;
    }

    @Override
    public void visit(final String name, final Object value) {
        if (name.contains("CalledViaReflection")) {
            m_skipper.skip();
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String name, final String desc) {
        return null;
    }

    @Override
    public AnnotationVisitor visitArray(final String name) {
        return null;
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public void visitEnum(final String name, final String desc, final String value) {
    }
}