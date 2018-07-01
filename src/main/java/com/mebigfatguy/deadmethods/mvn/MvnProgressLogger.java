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

import org.apache.maven.plugin.logging.Log;

import com.mebigfatguy.deadmethods.ProgressLogger;

public class MvnProgressLogger implements ProgressLogger {

    private Log log;

    public MvnProgressLogger(FDMMojo mojo) {
        log = mojo.getLog();
    }

    @Override
    public void log(String message) {
        log.error(message);

    }

    @Override
    public void verbose(String message) {
        log.debug(message);
    }
}
