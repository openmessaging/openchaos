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

package io.openchaos.driver.queue;

import io.openchaos.driver.ChaosDriver;

public interface PubSubDriver extends ChaosDriver {

    /**
     * Create a new topic with a given number of partitions
     */
    void createTopic(String topic, int partitions);

    /**
     * Create a Producer for a given topic. Producer will apply operations to the cluster to be tested
     */
    MQChaosProducer createProducer(String topic);

    /**
     * Create a PushConsumer. Note: if driver use pull consumer, you can choose not to implement this method
     */
    MQChaosPushConsumer createPushConsumer(String topic,
        String subscriptionName,
        ConsumerCallback consumerCallback);

    /**
     * Create a PullConsumer. Note: if driver use push consumer, you can choose not to implement this method
     */
    MQChaosPullConsumer createPullConsumer(String topic, String subscriptionName);

}
