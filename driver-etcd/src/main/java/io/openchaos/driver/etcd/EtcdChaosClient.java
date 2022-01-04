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

package io.openchaos.driver.etcd;

import com.google.common.base.Charsets;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.openchaos.common.InvokeResult;
import io.openchaos.driver.kv.KVClient;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
public class EtcdChaosClient implements KVClient {

    private final Client client;

    public EtcdChaosClient(Client client) {
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    @Override
    public void start() {

    }

    @Override
    public void close() {
        Optional.ofNullable(client).ifPresent(c -> c.close());
        log.info("Close etcd client");
    }

    @Override
    public InvokeResult put(Optional<String> key, String value) {
        try {
            PutResponse response = client.getKVClient()
                .put(ByteSequence.from(key.get() + value, Charsets.UTF_8), ByteSequence.from(value, Charsets.UTF_8))
                .get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Etcd put failed.", e);
            return InvokeResult.FAILURE;
        }
        return InvokeResult.SUCCESS;
    }

    @Override
    public List<String> getAll(Optional<String> key, int putInvokeCount) {
        List<String> results = new LinkedList<>();
        KV kv = client.getKVClient();

        try {
            for (int i = 0; i < putInvokeCount; i++) {
                GetResponse response = kv.get(ByteSequence.from(key.get() + i, Charsets.UTF_8)).get();
                if (response.getKvs().isEmpty()) {
                    continue;
                }
                for (KeyValue keyValue : response.getKvs()) {
                    results.add(keyValue.getValue().toString(Charsets.UTF_8));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Get etcd key failed.", e);
        }
        return results;
    }

    @Override
    public List<String> getAll(Optional<String> key) {
        return getAll(key, 1);
    }
}
