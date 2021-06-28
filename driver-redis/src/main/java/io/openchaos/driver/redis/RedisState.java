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

package io.openchaos.driver.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.openchaos.driver.ChaosState;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class RedisState implements ChaosState {
    RedisSentinelCommands<String, String> sentinel;
    String masterId;

    @Override
    public void initialize(String masterId, String addr) {
        String[] ip = addr.split(":");
        String host = ip[0];
        int port = Integer.parseInt(ip[1]);
        RedisURI sentinelUri = RedisURI.Builder.sentinel(host, port, masterId).build();
        RedisClient redisClient = RedisClient.create();
        this.sentinel = redisClient.connectSentinel(sentinelUri).sync();
        this.masterId = masterId;
    }

    @Override
    public Set<String> getLeader() {
        InetSocketAddress socketAddress = (InetSocketAddress) sentinel.getMasterAddrByName(masterId);
        Set<String> leaderAddr = new HashSet<>();
        leaderAddr.add(socketAddress.getHostName());
        return leaderAddr;
    }
}