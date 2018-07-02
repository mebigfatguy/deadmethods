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
package com.mebigfatguy.deadmethods.mvn;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import com.mebigfatguy.deadmethods.ClassPath;

public class MvnAuxClassPath implements ClassPath {

    private List<MavenProject> projects;
    private String localRepo;

    public MvnAuxClassPath(List<MavenProject> projects, Settings settings) {
        this.projects = projects;
        localRepo = settings.getLocalRepository();
    }

    @Override
    public Iterator<String> iterator() {
        return new MvnAuxPathIterator();
    }

    class MvnAuxPathIterator implements Iterator<String> {
        Iterator<Dependency> mvnIterator;

        public MvnAuxPathIterator() {
            Set<Dependency> dependencies = new HashSet<>();
            for (MavenProject module : projects) {
                dependencies.addAll(module.getDependencies());
            }
            mvnIterator = dependencies.iterator();
        }

        @Override
        public boolean hasNext() {
            return mvnIterator.hasNext();
        }

        @Override
        public String next() {
            Dependency dependency = mvnIterator.next();

            return localRepo + dependency.getGroupId().replace('.', '/') + "/" + dependency.getArtifactId() + "/" + dependency.getVersion() + "/"
                    + dependency.getArtifactId() + "-" + dependency.getVersion() + "." + dependency.getType();
        }
    }
}
