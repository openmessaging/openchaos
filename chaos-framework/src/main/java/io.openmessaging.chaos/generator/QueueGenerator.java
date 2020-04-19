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

package io.openmessaging.chaos.generator;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class QueueGenerator {

    private static final AtomicLong stagger = new AtomicLong(0);

    private static final Random random = new Random();

    public static QueueOperation generate() {
        if (random.nextDouble() < 0.5) {
            return new QueueOperation("enqueue", String.valueOf(stagger.getAndIncrement()));
        } else {
            return new QueueOperation("dequeue");
        }
    }
}
