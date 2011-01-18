package com.mebigfatguy.deadmethods;

import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class CalledMethodRemovingMethodVisitor implements MethodVisitor {

	private final FindDeadMethods findDeadMethods;

	public CalledMethodRemovingMethodVisitor(FindDeadMethods findDeadMethods) {
		this.findDeadMethods = findDeadMethods;
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
        this.findDeadMethods.methodNames.remove(owner + ":" + name + desc);
        removeDerivedCalls(owner, name + desc);
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
        return null;
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label[] labels) {
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

    private void removeDerivedCalls(final String clsName, final String methodDesc) {
        Set<String> children = this.findDeadMethods.hierarchy.get(clsName);
        if (children != null) {
            for (String child : children) {
                this.findDeadMethods.methodNames.remove(child + ":" + methodDesc);
                removeDerivedCalls(child, methodDesc);
            }
        }
    }
}