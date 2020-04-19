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

import io.openmessaging.chaos.common.InvokeResult;
import io.openmessaging.chaos.common.Message;
import io.openmessaging.chaos.driver.ChaosClient;
import java.util.List;

public interface MQChaosClient extends ChaosClient {

    /**
     * Enqueue a value to mq cluster
     *
     * @param value
     * @return result of enqueue
     */
    InvokeResult enqueue(String value);

    /**
     * Enqueue a value with sharding key to mq cluster
     *
     * @param value
     * @return result of enqueue
     */
    InvokeResult enqueue(String shardingKey, String value);

    /**
     * Dequeue from mq cluster
     */
    List<Message> dequeue();

}
