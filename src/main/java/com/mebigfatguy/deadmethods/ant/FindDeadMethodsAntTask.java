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
package com.mebigfatguy.deadmethods.ant;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

import com.mebigfatguy.deadmethods.DeadMethods;
import com.mebigfatguy.deadmethods.IgnoredClass;
import com.mebigfatguy.deadmethods.IgnoredMethod;
import com.mebigfatguy.deadmethods.IgnoredPackage;
import com.mebigfatguy.deadmethods.ReflectiveAnnotation;

public class FindDeadMethodsAntTask extends Task {

    Path path;
    Path auxPath;
    Set<IgnoredPackage> ignoredPackages = new HashSet<>();
    Set<IgnoredClass> ignoredClasses = new HashSet<>();
    Set<IgnoredMethod> ignoredMethods = new HashSet<>();
    Set<ReflectiveAnnotation> reflectiveAnnotations = new HashSet<>();

    public void addConfiguredClasspath(final Path classpath) {
        path = classpath;
    }

    public void addConfiguredAuxClasspath(final Path auxClassPath) {
        auxPath = auxClassPath;
    }

    public IgnoredPackage createIgnoredPackage() {
        IgnoredPackage ip = new IgnoredPackage();
        ignoredPackages.add(ip);
        return ip;
    }

    public IgnoredClass createIgnoredClass() {
        IgnoredClass ic = new IgnoredClass();
        ignoredClasses.add(ic);
        return ic;
    }

    public IgnoredMethod createIgnoredMethod() {
        IgnoredMethod im = new IgnoredMethod();
        ignoredMethods.add(im);
        return im;
    }

    public ReflectiveAnnotation createReflectiveAnnotation() {
        ReflectiveAnnotation ra = new ReflectiveAnnotation();
        reflectiveAnnotations.add(ra);
        return ra;
    }

    @Override
    public void execute() throws BuildException {
        if (path == null) {
            throw new BuildException("classpath attribute not set");
        }

        if (auxPath == null) {
            auxPath = new Path(getProject());
        }

        try {

            DeadMethods dm = new DeadMethods(new AntProgressLogger(this), new AntClassPath(path), new AntClassPath(auxPath), ignoredPackages, ignoredClasses,
                    ignoredMethods);

            Set<String> allMethods = dm.getDeadMethods();

            Project p = getProject();
            for (String m : allMethods) {
                p.log(m);
            }
        } catch (Exception e) {
            throw new BuildException("Finding dead methods failed", e);
        }
    }

    /** for testing only */
    public static void main(String[] args) {

        if (args.length < 1) {
            throw new IllegalArgumentException("args (" + Arrays.toString(args) + ") must contain classpath root");
        }

        FindDeadMethodsAntTask fdm = new FindDeadMethodsAntTask();
        Project project = new Project();
        fdm.setProject(project);

        Path path = new Path(project);
        path.setLocation(new File(args[0]));
        fdm.addConfiguredClasspath(path);
        ReflectiveAnnotation ra = fdm.createReflectiveAnnotation();
        ra.setName("test.reflective.ReflectiveUse");
        IgnoredPackage ip = fdm.createIgnoredPackage();
        ip.setPattern("test\\.ignored");

        project.addBuildListener(new BuildListener() {

            @Override
            public void buildFinished(BuildEvent event) {
            }

            @Override
            public void buildStarted(BuildEvent event) {
            }

            @Override
            public void messageLogged(BuildEvent event) {
                System.out.println(event.getMessage());
            }

            @Override
            public void targetFinished(BuildEvent event) {
            }

            @Override
            public void targetStarted(BuildEvent event) {
            }

            @Override
            public void taskFinished(BuildEvent event) {
            }

            @Override
            public void taskStarted(BuildEvent event) {
            }

        });
        fdm.execute();
    }
}
