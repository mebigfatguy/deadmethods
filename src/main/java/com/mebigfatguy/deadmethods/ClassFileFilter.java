/*
 * deadmethods - A unused methods detector
 * Copyright 2011-2017 MeBigFatGuy.com
 * Copyright 2011-2017 Dave Brosius
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