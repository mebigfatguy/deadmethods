package com.mebigfatguy.deadmethods;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.resources.FileResource;
import org.objectweb.asm.ClassReader;

public class FindDeadMethods extends Task {
    Path path;
    Map<String, Set<String>> hierarchy;
    Set<String> methodNames;
    Set<String> packages;
    MethodCollectingVisitor methodCollectingVisitor;
    CalledMethodRemovingClassVisitor calledMethodRemovingVisitor;

    public void addConfiguredClasspath(final Path classpath) {
        path = classpath;
    }
    
    public void setPackages(String packageList) {
    	packages = new HashSet<String>(Arrays.asList(packageList.split("\\s*,\\s*")));
    }

    @Override
    public void execute() throws BuildException {
        if (path == null) {
            throw new BuildException("classpath attribute not set");
        }
        
        if (packages == null) {
        	throw new BuildException("packages attribute not set");
        }

        methodNames = new TreeSet<String>();
        hierarchy = new HashMap<String, Set<String>>();
        methodCollectingVisitor =  new MethodCollectingVisitor(this);
        calledMethodRemovingVisitor = new CalledMethodRemovingClassVisitor(this);

        collectMethodNames();
        findDeadMethods();
        for (String dm : methodNames) {
            log(dm);
        }
    }

    private void collectMethodNames() throws BuildException
    {
        Iterator<FileResource> it = path.iterator();
        while (it.hasNext()) {
            FileResource fr = it.next();
            File dir = fr.getFile();
            if (dir.isFile()) {
                throw new BuildException("Only directories are supported: " + dir.getPath());
            }

            collectMethodNamesFromDir(dir);
        }
    }

    private void collectMethodNamesFromDir(final File dir) {
        File[] files = dir.listFiles(new ClassFileFilter());
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    collectMethodNamesFromDir(f);
                } else {
                    InputStream is = null;
                    try {
                        is = new BufferedInputStream(new FileInputStream(f));
                        ClassReader cr = new ClassReader(is);
                        cr.accept(methodCollectingVisitor, ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG);
                    } catch (IOException fnfe) {
                        throw new BuildException("Failed reading method names from: " + f, fnfe);
                    } finally {
                        Closer.close(is);
                    }
                }
            }
        }
    }

    private void findDeadMethods() {
        Iterator<FileResource> it = path.iterator();
        while (it.hasNext()) {
            FileResource fr = it.next();
            File dir = fr.getFile();
            if (dir.isFile()) {
                throw new BuildException("Only directories are supported: " + dir.getPath());
            }

            removeCalledMethodsFromDir(dir);
        }
    }

    private void removeCalledMethodsFromDir(final File dir) {
        File[] files = dir.listFiles(new ClassFileFilter());
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    removeCalledMethodsFromDir(f);
                } else {
                    InputStream is = null;
                    try {
                        is = new BufferedInputStream(new FileInputStream(f));
                        ClassReader cr = new ClassReader(is);
                        cr.accept(calledMethodRemovingVisitor, ClassReader.SKIP_DEBUG);
                    } catch (IOException fnfe) {
                        throw new BuildException("Failed removing method names from: " + f, fnfe);
                    } finally {
                        Closer.close(is);
                    }
                }
            }
        }
    }
}

