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
package com.mebigfatguy.deadmethods;

import org.apache.tools.ant.types.Path;

public class PathIterator extends AbstractClassPathIterator {
	String extension;

	public PathIterator(Path classPath, String fileExtension) {
		super(classPath);
		extension = fileExtension;
	}

    @Override
    public boolean validPath(String path, boolean isDirectory) {
        if (isDirectory) {
            return true;
        }
        
        return path.endsWith(extension);
    }
    
    @Override
    public String adjustPath(String path) {
        path =  path.substring(0, path.length() - extension.length());
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }
}
