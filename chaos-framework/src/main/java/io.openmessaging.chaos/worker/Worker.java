/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.openmessaging.chaos.worker;

import org.slf4j.Logger;

public abstract class Worker extends Thread {

    private volatile boolean breakFlag = false;

    protected Logger logger;

    public Worker(String name, Logger logger) {
        super(name);
        this.logger = logger;
    }

    public abstract void loop() throws Exception;

    public void await(long interval) throws InterruptedException {
        sleep(interval);
    }

    public void breakLoop() {
        breakFlag = true;
    }

    @Override
    public void run() {
        while (!breakFlag) {
            try {
                loop();
            } catch (InterruptedException e) {
                if (logger != null) {
                    logger.info("{} break loop", getName());
                }
            } catch (Throwable t) {
                if (logger != null) {
                    logger.error("Unexpected Error in running {} ", getName(), t);
                }
            }
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

}
