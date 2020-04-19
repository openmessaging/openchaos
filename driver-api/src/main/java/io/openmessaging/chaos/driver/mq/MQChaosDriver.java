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

package io.openmessaging.chaos.driver.mq;

import io.openmessaging.chaos.driver.ChaosDriver;
import java.util.List;

public interface MQChaosDriver extends ChaosDriver {

    /**
     * Create a new topic with a given number of partitions
     */
    void createTopic(String topic, int partitions);

    /**
     * Create a ChaosClient for a given topic. ChaosClient will apply operations to the cluster to be tested
     */
    MQChaosClient createChaosClient(String topic);

    /**
     * Create a ChaosNode. ChaosNode represents one of the nodes in the cluster to be tested
     *
     * @param node current node
     * @param nodes all the nodes of the distributed system to be tested
     */
    MQChaosNode createChaosNode(String node, List<String> nodes);
}
