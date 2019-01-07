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
package com.mebigfatguy.deadmethods;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

public abstract class AbstractClassPathIterator implements Iterator<String> {

    Iterator<String> frIt;
    Iterator<String> subIt = null;
    ProgressLogger logger;

    public AbstractClassPathIterator(ClassPath classPath, ProgressLogger progressLogger) {

        frIt = classPath.iterator();
        logger = progressLogger;
    }

    public abstract boolean validPath(String path, boolean isDirectory);

    public abstract String adjustPath(String path);

    @Override
    public boolean hasNext() {
        do {
            if (subIt == null) {
                initializeSubIterator();
            }

            if (subIt.hasNext()) {
                return true;
            }

            subIt = null;
        } while (frIt.hasNext());

        return false;
    }

    @Override
    public String next() {
        do {
            if (subIt == null) {
                initializeSubIterator();
            }

            if (subIt.hasNext()) {
                return subIt.next();
            }

            subIt = null;
        } while (frIt.hasNext());
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove not supported");
    }

    private void initializeSubIterator() {
        while ((subIt == null) && frIt.hasNext()) {
            try {
                String fr = frIt.next();
                Path dir = Paths.get(fr);
                if (!Files.isDirectory(dir)) {
                    Path jar = dir;
                    if (jar.toString().endsWith(".jar")) {
                        subIt = new JarIterator(jar);
                    }
                } else {
                    subIt = new DirectoryIterator(dir);
                }
            } catch (IOException ioe) {
                // hasNext() will return false/next() will throw
            }
        }

        if (subIt == null) {
            subIt = new Iterator<String>() {

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public String next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    class JarIterator implements Iterator<String> {

        private JarInputStream jis;
        private String nextEntry;

        public JarIterator(Path jar) throws IOException {
            jis = new JarInputStream(new BufferedInputStream(Files.newInputStream(jar)));
            nextEntry = null;
        }

        @Override
        public boolean hasNext() {
            if (nextEntry == null) {
                nextEntry = getNextEntry();
            }

            if (nextEntry == null) {
                Closer.close(jis);
                jis = null;
            }

            return nextEntry != null;
        }

        @Override
        public String next() {
            if (nextEntry == null) {
                nextEntry = getNextEntry();
            }

            if (nextEntry == null) {
                Closer.close(jis);
                jis = null;
                throw new NoSuchElementException();
            }
            String className = nextEntry;
            nextEntry = null;
            return className;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private String getNextEntry() {
            if (jis == null) {
                return null;
            }

            try {
                JarEntry entry = jis.getNextJarEntry();

                while (entry != null) {
                    if (validPath(entry.getName(), false)) {
                        return adjustPath(entry.getName());
                    }

                    entry = jis.getNextJarEntry();
                }
                return null;
            } catch (IOException ioe) {
                return null;
            }
        }
    }

    class DirectoryIterator implements Iterator<String> {
        private final String root;
        private final List<Path> paths;
        private String nextFile;

        public DirectoryIterator(Path dir) {
            root = dir.toAbsolutePath().toString();
            paths = new ArrayList<>();
            paths.add(dir);
            nextFile = null;
        }

        @Override
        public boolean hasNext() {
            if (nextFile == null) {
                try {
                    nextFile = getNextFile();
                } catch (IOException e) {
                    return false;
                }
            }

            if (nextFile == null) {
                paths.clear();
            }

            return nextFile != null;
        }

        @Override
        public String next() {
            if (nextFile == null) {
                try {
                    nextFile = getNextFile();
                } catch (IOException e) {
                    NoSuchElementException nsee = new NoSuchElementException("failed to fetch file");
                    nsee.initCause(e);
                    throw nsee;
                }
            }

            if (nextFile == null) {
                paths.clear();
                throw new NoSuchElementException();
            }
            String className = nextFile;
            nextFile = null;
            return className;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private String getNextFile() throws IOException {

            while (!paths.isEmpty()) {

                Path file = paths.remove(paths.size() - 1);
                if (Files.exists(file)) {
                    if (Files.isDirectory(file)) {
                        List<Path> children = Files.list(file).filter(f -> {
                            String name = f.toString();
                            name = name.substring(root.length());
                            return validPath(name, Files.isDirectory(f));
                        }).collect(Collectors.toList());

                        if (children != null) {
                            paths.addAll(children);
                        }
                    } else {
                        String fileName = file.toAbsolutePath().toString();
                        fileName = "/" + fileName.substring(root.length() + 1).replaceAll("\\\\", "/");
                        if (validPath(fileName, Files.isDirectory(file))) {
                            return adjustPath(fileName);
                        }
                    }
                } else {
                    logger.log("Classpath element doesn't exist - ignored: " + file.toString());
                }
            }

            return null;
        }
    }

}
