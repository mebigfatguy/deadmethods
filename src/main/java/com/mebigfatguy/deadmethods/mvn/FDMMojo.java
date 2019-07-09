/*
 * deadmethods - A unused methods detector
 * Copyright 2011-2019 MeBigFatGuy.com
 * Copyright 2011-2019 Dave Brosius
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
package com.mebigfatguy.deadmethods.mvn;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import com.mebigfatguy.deadmethods.DeadMethods;
import com.mebigfatguy.deadmethods.IgnoredClass;
import com.mebigfatguy.deadmethods.IgnoredMethod;
import com.mebigfatguy.deadmethods.IgnoredPackage;

@Mojo(name = "finddeadmethods", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class FDMMojo extends AbstractMojo {

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Parameter(property = "session", readonly = true, required = true)
    private MavenSession session;

    @Parameter(property = "outputFile")
    private File outputFile;

    @Override
    public void execute() throws MojoExecutionException {

        List<MavenProject> projects = session.getProjectDependencyGraph().getSortedProjects();

        try {

            Set<IgnoredPackage> ignoredPackages = null;
            Set<IgnoredClass> ignoredClasses = null;
            Set<IgnoredMethod> ignoredMethods = null;

            DeadMethods dm = new DeadMethods(new MvnProgressLogger(this), new MvnClassPath(projects), new MvnAuxClassPath(projects, settings), ignoredPackages,
                    ignoredClasses, ignoredMethods);

            Set<String> allMethods = dm.getDeadMethods();

            Log log = getLog();
            for (String m : allMethods) {
                log.error(m);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Finding dead methods failed", e);
        }
    }
}
