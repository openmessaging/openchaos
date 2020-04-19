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

package io.openmessaging.chaos.driver;

public interface ChaosNode {

    /**
     * Set up the distributed system program to be tested on this particular node. No need to start process
     */
    void setup();

    /**
     * Teardown the distributed system program to be tested on this particular node
     */
    void teardown();

    /**
     * Start specified processes on this particular node
     */
    void start();

    /**
     * Stop specified processes on this particular node
     */
    void stop();

    /**
     * Kill specified processes on this particular node
     */
    void kill();

    /**
     * Pause specified processes on this particular node
     */
    void pause();

    /**
     * Resume specified processes on this particular node
     */
    void resume();

}
