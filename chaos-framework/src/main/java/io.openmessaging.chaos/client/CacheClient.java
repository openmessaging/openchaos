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

package io.openmessaging.chaos.client;

import java.util.List;
import io.openmessaging.chaos.common.InvokeResult;
import io.openmessaging.chaos.driver.cache.CacheChaosClient;
import io.openmessaging.chaos.driver.cache.CacheChaosDriver;
import io.openmessaging.chaos.generator.Operation;
import io.openmessaging.chaos.generator.SequenceGenerator;
import io.openmessaging.chaos.recorder.Recorder;
import io.openmessaging.chaos.recorder.RequestLogEntry;
import io.openmessaging.chaos.recorder.ResponseLogEntry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheClient implements Client {

    private static final AtomicInteger CLIENT_ID_GENERATOR = new AtomicInteger(0);
    private static final Logger log = LoggerFactory.getLogger(CacheClient.class);

    private CacheChaosClient cacheChaosClient;
    private CacheChaosDriver cacheChaosDriver;
    private Recorder recorder;
    private int clientId;

    private Optional<String> key;

    public CacheClient(CacheChaosDriver cacheChaosDriver, Recorder recorder, Optional<String> key) {
        this.cacheChaosDriver = cacheChaosDriver;
        this.recorder = recorder;
        this.clientId = CLIENT_ID_GENERATOR.getAndIncrement();
        this.key = key;
    }

    @Override public void setup() {
        if (cacheChaosDriver == null) {
            throw new IllegalArgumentException("cacheChaosDriver is null when setup CacheClient");
        }
        cacheChaosClient = cacheChaosDriver.createCacheChaosClient();
        cacheChaosClient.start();
    }

    @Override public void teardown() {
        cacheChaosClient.close();
    }

    @Override public void nextInvoke() {
        Operation op = SequenceGenerator.generateCacheOperation();
        RequestLogEntry requestLogEntry = new RequestLogEntry(clientId, op.getInvokeOperation(), op.getValue(), System.currentTimeMillis());
        recorder.recordRequest(requestLogEntry);
        InvokeResult result = cacheChaosClient.put(key, op.getValue());
        recorder.recordResponse(new ResponseLogEntry(clientId, op.getInvokeOperation(), result, op.getValue(), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
    }

    @Override public void lastInvoke() {
        RequestLogEntry requestLogEntry = new RequestLogEntry(clientId, "getAll", null, System.currentTimeMillis());
        recorder.recordRequest(requestLogEntry);
        List<String> results = cacheChaosClient.getAll(key);
        if (results != null && !results.isEmpty()) {
            recorder.recordResponse(new ResponseLogEntry(clientId, "getAll", InvokeResult.SUCCESS, results.toString(), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
        } else {
            recorder.recordResponse(new ResponseLogEntry(clientId, "getAll", InvokeResult.FAILURE, null, System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
        }
    }
}
