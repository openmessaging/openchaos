/*
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

package io.openchaos.model;

import io.openchaos.DriverConfiguration;
import io.openchaos.driver.ChaosNode;
import java.util.Map;

public interface Model {

    /**
     * Set up all the clients
     */
    void setupClient();

    /**
     * Set up cluster to be tested
     *
     * @param driverConfiguration driver configuration
     * @param isInstall whether to reinstall the program on each node
     * @return
     */
    Map<String, ChaosNode> setupCluster(DriverConfiguration driverConfiguration, boolean isInstall);

    /**
     * Ensure cluster are ready
     */
    boolean probeCluster();

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

    /**
     * Get address of MetaNode
     */
    String getMetaNode();

    /**
     * Get Name of a cluster that can be distinguished. eg: ClusterName of RocketMQ, MasterID of Redis
     */
    String getMetaName();
}
