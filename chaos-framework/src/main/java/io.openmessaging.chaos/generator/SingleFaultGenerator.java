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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SingleFaultGenerator implements FaultGenerator {

    private Collection<String> nodes;

    private String mode;

    private Random random = new Random();

    public SingleFaultGenerator(Collection<String> nodes, String mode) {
        this.nodes = nodes;
        this.mode = mode;
    }

    @Override
    public List<FaultOperation> generate() {
        List<FaultOperation> operations = new ArrayList<>();
        int num = 0;
        switch (mode) {
            case "minor-suspend":
            case "minor-kill":
                num = nodes.size() % 2 == 0 ? nodes.size() / 2 - 1 : nodes.size() / 2;
                break;
            case "major-suspend":
            case "major-kill":
                num = nodes.size() % 2 == 0 ? nodes.size() / 2 : nodes.size() / 2 + 1;
                break;
            case "random-suspend":
            case "random-kill":
                num = random.nextInt(nodes.size()) + 1;
                break;

        }
        List<String> shuffleNodes = new ArrayList<>(nodes);
        Collections.shuffle(shuffleNodes);
        for (int i = 0; i < num; i++) {
            FaultOperation operation = new FaultOperation(mode, shuffleNodes.get(i));
            operations.add(operation);
        }
        return operations;
    }
}
