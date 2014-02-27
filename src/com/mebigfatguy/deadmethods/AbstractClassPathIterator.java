/*
 * deadmethods - A unused methods detector
 * Copyright 2011-2013 MeBigFatGuy.com
 * Copyright 2011-2013 Dave Brosius
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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;

public abstract class AbstractClassPathIterator implements Iterator<String> {

    Iterator<Resource> frIt;
    Iterator<String> subIt = null;

    public AbstractClassPathIterator(Path classPath) {
        frIt = classPath.iterator();
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
        } while ((subIt == null) && frIt.hasNext());

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
        } while ((subIt == null) && frIt.hasNext());
        throw new NoSuchElementException();
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove not supported");
    }

    private void initializeSubIterator() {
        while (frIt.hasNext() && (subIt == null)) {
            try {
                Resource fr = frIt.next();
                File dir = new File(fr.toString());
                if (dir.isFile()) {
                    File jar = dir;
                    if (jar.getName().endsWith(".jar")) {
                        subIt = new JarIterator(jar);
                    }
                } else {
                    subIt = new DirectoryIterator(dir);
                }
            } catch (IOException ioe) {

            }
        }

        if (subIt == null)
        {
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

        public JarIterator(File jar) throws IOException {
            jis = new JarInputStream(new BufferedInputStream(new FileInputStream(jar)));
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
                    if (validPath(entry.getName(), entry.isDirectory())) {
                        return entry.getName();
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
        private final List<File> paths;
        private String nextFile;

        public DirectoryIterator(File dir) {
            root = dir.getAbsolutePath();
            paths = new ArrayList<File>();
            paths.add(dir);
            nextFile = null;
        }

        @Override
        public boolean hasNext() {
            if (nextFile == null) {
                nextFile = getNextFile();
            }

            if (nextFile == null) {
                paths.clear();
            }

            return nextFile != null;
        }

        @Override
        public String next() {
            if (nextFile == null) {
                nextFile = getNextFile();
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

        private String getNextFile() {

            while (!paths.isEmpty()) {

                File file = paths.remove(paths.size() - 1);
                if (file.exists()) {
                    if (file.isFile()) {
                        String fileName = file.getAbsolutePath();
                        fileName = "/" + fileName.substring(root.length() + 1).replaceAll("\\\\", "/");
                        if (validPath(fileName, file.isDirectory())) {
                            return adjustPath(fileName);
                        }
                    } else {
                        File[] files = file.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                String name = f.getPath();
                                name = name.substring(root.length());
                                return validPath(f.getPath(), f.isDirectory());
                            }
                        });
                        paths.addAll(Arrays.asList(files));
                    }
                } else {
                    TaskFactory.getTask().log("Classpath element doesn't exist - ignored: " + file.getPath());
                }
            }

            return null;
        }
    }


}
