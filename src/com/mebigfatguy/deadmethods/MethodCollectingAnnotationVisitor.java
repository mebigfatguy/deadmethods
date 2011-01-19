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