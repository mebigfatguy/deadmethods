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

import org.apache.tools.ant.types.Path;

public class PathPrefixIterator extends AbstractClassPathIterator {
    String pathPrefix;

    @SuppressWarnings("unchecked")
    public PathPrefixIterator(Path classPath, String prefix) {
        super(classPath);
        pathPrefix = prefix;
    }

    @Override
    public boolean validPath(String path, boolean isDirectory) {
        if (isDirectory) {
            if (pathPrefix.startsWith(path)) {
                return true;
            }
        }
        return path.startsWith(pathPrefix);
    }
    
    @Override
    public String adjustPath(String path) {
        return path.substring(1);
    }
}
