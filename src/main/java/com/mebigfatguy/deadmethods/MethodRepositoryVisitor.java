/*
 * deadmethods - A unused methods detector
 * Copyright 2011-2018 MeBigFatGuy.com
 * Copyright 2011-2018 Dave Brosius
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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodRepositoryVisitor extends MethodVisitor {

	private final MethodInfo methodInfo;

	public MethodRepositoryVisitor(MethodInfo minfo) {
	    super(Opcodes.ASM5);
		methodInfo = minfo;
	}

	@Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        if ("Lorg/junit/Test;".equals(desc)
        ||  "Lorg/junit/Before;".equals(desc)
        ||  "Lorg/junit/After;".equals(desc)) {
        	methodInfo.setTest(true);
        }

        String annotationName = desc.substring(1, desc.length() - 1).replaceAll("/", ".");
        methodInfo.addAnnotation(annotationName);
        return null;
    }
}
