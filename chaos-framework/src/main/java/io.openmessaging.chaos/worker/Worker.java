/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.openmessaging.chaos.worker;

import org.slf4j.Logger;

public abstract class Worker extends Thread {

    protected Logger log;
    private volatile boolean breakFlag = false;

    public Worker(String name, Logger log) {
        super(name);
        this.log = log;
    }

    public abstract void loop() throws Exception;

    protected void await(long interval) throws InterruptedException {
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
                if (log != null) {
                    log.info("{} break loop", getName());
                }
            } catch (Throwable t) {
                if (log != null) {
                    log.error("Unexpected Error in running {} ", getName(), t);
                }
            }
        }
    }

    public Logger getLogger() {
        return log;
    }

    public void setLogger(Logger log) {
        this.log = log;
    }

}
