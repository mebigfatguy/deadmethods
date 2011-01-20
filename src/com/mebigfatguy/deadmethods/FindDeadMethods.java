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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.objectweb.asm.ClassReader;

public class FindDeadMethods extends Task {
    Path path;
    Path auxPath;

    public void addConfiguredClasspath(final Path classpath) {
        path = classpath;
    }

    public void addConfiguredAuxClasspath(final Path auxClassPath) {
    	auxPath = auxClassPath;
    }

    @Override
    public void execute() throws BuildException {
        if (path == null) {
            throw new BuildException("classpath attribute not set");
        }

        if (auxPath == null) {
        	auxPath = new Path(getProject());
        }

        ClassRepository repo = new ClassRepository(path, auxPath);
        Set<String> allMethods = new HashSet<String>();
        try {
	        for (String className : repo) {
	        	ClassInfo classInfo = repo.getClassInfo(className);
        		Set<MethodInfo> methods = classInfo.getMethodInfo();

	        	for (MethodInfo methodInfo : methods) {
	        		allMethods.add(className + ":" + methodInfo.getMethodName() + methodInfo.getMethodSignature());
	        	}
	        }

	        // Remove methods in Object
	        {
	        	ClassInfo info = repo.getClassInfo("java/lang/Object");
	        	for (MethodInfo methodInfo : info.getMethodInfo()) {
        			clearDerivedMethods(allMethods, info, methodInfo.getMethodName() + methodInfo.getMethodSignature());
        		}
	        }

	        // Remove interface methods implemented in classes that implement the interface
	        for (ClassInfo classInfo : repo.getAllClassInfos()) {
	        	if (classInfo.isInterface()) {
	        		for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
	        			clearDerivedMethods(allMethods, classInfo, methodInfo.getMethodName() + methodInfo.getMethodSignature());
	        		}
	        	}
	        }

	        for (String className : repo) {
	        	InputStream is = null;
	        	try {
	        		is = repo.getClassStream(className);

	        		ClassReader r = new ClassReader(is);
	        		r.accept(new CalledMethodRemovingClassVisitor(repo, allMethods), ClassReader.SKIP_DEBUG);
	        	} finally {
	        		Closer.close(is);
	        	}
	        }

	        for (String m : allMethods) {
	        	System.out.println(m);
	        }

        } catch (IOException ioe) {
        	throw new BuildException("Failed collecting methods", ioe);
        }
    }

    private void clearDerivedMethods(Set<String> methods, ClassInfo info, String methodInfo) throws IOException {
    	Set<ClassInfo> derivedInfos = info.getDerivedClasses();

    	for (ClassInfo derivedInfo : derivedInfos) {
    		methods.remove(derivedInfo.getClassName() + ":" + methodInfo);
    		clearDerivedMethods(methods, derivedInfo, methodInfo);
    	}
    }

    /** for testing only */
    public static void main(String[] args) {
    	FindDeadMethods fdm = new FindDeadMethods();
    	Project project = new Project();
    	fdm.setProject(project);

    	Path path = new Path(project);
    	path.setLocation(new File("/home/dave/dev/deadmethods/classes"));
    	fdm.addConfiguredClasspath(path);

    	Path auxpath = new Path(project);
    	auxpath.setLocation(new File("/home/dave/dev/deadmethods/lib/ant-1.8.1.jar"));
    	auxpath.setLocation(new File("/home/dave/dev/deadmethods/lib/asm-3.3.jar"));
    	fdm.addConfiguredAuxClasspath(auxpath);

    	fdm.execute();
    }
}

