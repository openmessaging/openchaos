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

package io.openmessaging.chaos.model;

import io.openmessaging.chaos.driver.mq.MQChaosNode;
import java.util.List;
import java.util.Map;

public interface Model {

    /**
     * Set up all the clients
     */
    void setupClient(boolean isOrderTest, List<String> shardingKeys);

    /**
     * Set up cluster to be tested
     *
     * @param nodes all the nodes of cluster to be tested
     * @param isInstall whether to reinstall the program on each node
     * @return
     */
    Map<String, MQChaosNode> setupCluster(List<String> nodes, boolean isInstall);

    /**
     * Start the test
     */
    void start();

    /**
     * Stop the test
     */
    void stop();

    /**
     * Do something after stop the test
     */
    void afterStop();

    /**
     * Shutdown the model
     */
    void shutdown();

}
