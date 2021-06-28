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
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.openchaos.common.InvokeResult;
import io.openchaos.driver.kv.KVClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RedisChaosClient implements KVClient {

    private static final Logger log = LoggerFactory.getLogger(RedisClient.class);
    private final RedisClient redisClient;
    private final StatefulRedisMasterReplicaConnection<String, String> connection;
    private static RedisAsyncCommands<String, String> asyncCommands;

    public RedisChaosClient(RedisClient redisClient, StatefulRedisMasterReplicaConnection<String, String> connection) {
        this.redisClient = redisClient;
        this.connection = connection;
    }

    @Override
    public void start() {
        if (connection != null) {
            asyncCommands = connection.async();

        }
    }


    @Override
    public void close() {
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    @Override
    public InvokeResult put(Optional<String> key, String value) {
        try {
            RedisFuture<String> redisFuture = asyncCommands.set(key + value, value);
            String result = redisFuture.get();
            if (result == null) {
                log.warn("enqueue error");
                return InvokeResult.FAILURE;
            }
        } catch (InterruptedException e) {
            log.warn("enqueue error", e);
            return InvokeResult.FAILURE;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.warn("enqueue timeout...", e);
                return InvokeResult.UNKNOWN;
            }
            return InvokeResult.FAILURE;
        }
        return InvokeResult.SUCCESS;
    }

    @Override
    public List<String> getAll(Optional<String> key, int putInvokeCount) {
        List<String> values = new ArrayList<>();
        try {
            for (int i = 0; i < putInvokeCount ; i++) {
                RedisFuture<String> redisFuture = asyncCommands.get(String.valueOf(key) + i);
                values.add(redisFuture.get(1, TimeUnit.MINUTES));
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return values;
    }

    @Override
    public List<String> getAll(Optional<String> key) {
        return null;
    }
}