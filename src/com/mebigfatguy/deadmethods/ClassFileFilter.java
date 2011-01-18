package com.mebigfatguy.deadmethods;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ClassFileFilter implements FilenameFilter {
    Pattern m_testPattern = Pattern.compile(".*Test((Support|Case))?(\\$[^\\.]+)?\\.class");
    @Override
    public boolean accept(final File dir, final String name) {
        if (name.endsWith(".class")) {
            Matcher m = m_testPattern.matcher(name);
            if (!m.matches()) {
                return true;
            }
        }

        File f = new File(dir, name);
        return f.isDirectory();
    }
}