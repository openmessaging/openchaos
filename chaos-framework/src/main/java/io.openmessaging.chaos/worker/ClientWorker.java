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

package io.openmessaging.chaos.worker;

import com.google.common.util.concurrent.RateLimiter;
import io.openmessaging.chaos.client.Client;
import org.slf4j.Logger;

public class ClientWorker extends Worker {

    private RateLimiter rateLimiter;

    private Client client;

    public ClientWorker(String name, Client client, RateLimiter rateLimiter, Logger log) {
        super(name, log);
        this.client = client;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void loop() {
        rateLimiter.acquire();
        client.nextInvoke();
    }
}
