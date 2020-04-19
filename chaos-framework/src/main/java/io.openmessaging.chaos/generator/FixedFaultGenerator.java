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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FixedFaultGenerator implements FaultGenerator {

    private Collection<String> allNodes;

    private Collection<String> faultNodes;

    private String mode;

    public FixedFaultGenerator(Collection<String> allNodes, Collection<String> faultNodes, String mode) {
        this.allNodes = allNodes;
        this.faultNodes = faultNodes;
        this.mode = mode;
    }

    @Override
    public List<FaultOperation> generate() {
        List<FaultOperation> operations = new ArrayList<>();
        switch (mode) {
            case "fixed-kill":
            case "fixed-suspend":
                for (String node : faultNodes) {
                    operations.add(new FaultOperation(mode, node));
                }
                break;
            case "fixed-partition":
                Set<String> partition1 = new HashSet<>(faultNodes);
                Set<String> partition2 = new HashSet<>(allNodes);
                partition2.removeAll(partition1);
                allNodes.forEach(node -> {
                    if (partition1.contains(node)) {
                        operations.add(getPartitionOperation(node, partition2));
                    } else {
                        operations.add(getPartitionOperation(node, partition1));
                    }
                });
                break;
        }
        return operations;
    }

    private FaultOperation getPartitionOperation(String node, Set<String> partitionNodes) {
        List<String> invokeArgs = new ArrayList<>(partitionNodes);
        List<String> recoverArgs = new ArrayList<>(partitionNodes);
        return new FaultOperation(mode, node, invokeArgs, recoverArgs);
    }
}
