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
package com.mebigfatguy.deadmethods.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.mebigfatguy.deadmethods.ProgressLogger;

public class AntProgressLogger implements ProgressLogger {

    private Task task;

    public AntProgressLogger(Task t) {
        task = t;
    }

    @Override
    public void log(String message) {
        task.getProject().log(message);
    }

    @Override
    public void verbose(String message) {
        task.getProject().log(message, Project.MSG_VERBOSE);
    }
}
