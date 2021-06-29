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

package io.openchaos.generator;

import io.openchaos.driver.ChaosState;
import io.openchaos.driver.queue.QueueState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class FaultGenerator {

    private static List<String> faultList = Arrays.asList(
            "noop", "minor-kill", "major-kill", "random-kill", "fixed-kill", "leader-kill", "random-partition",
            "fixed-partition", "leader-partition", "partition-majorities-ring", "bridge", "minor-loss", "major-loss",
            "fixed-loss", "random-loss", "minor-delay", "major-delay", "random-delay", "fixed-delay",
            "minor-suspend", "major-suspend", "leader-suspend", "random-suspend", "fixed-suspend", "minor-cpu-high",
            "major-cpu-high", "random-cpu-high", "fixed-cpu-high", "minor-mem-high", "major-mem-high",
            "random-mem-high", "fixed-mem-high", "minor-disk-error", "major-disk-error",
            "random-disk-error", "fixed-disk-error", "minor-io-hang", "major-io-hang",
            "random-io-hang", "fixed-io-hang");
    private static Random random = new Random();
    private static ChaosState chaosState;

    public static List<String> getFaultList() {
        return faultList;
    }

    public static boolean isInFaultList(String faultName) {
        return faultList.contains(faultName);
    }

    public static List<FaultOperation> generate(Collection<String> nodes, String faultName) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Nodes can not be null or empty");
        }
        int num;
        switch (faultName) {
            case "noop":
                return Collections.EMPTY_LIST;
            case "minor-suspend":
            case "minor-kill":
            case "minor-cpu-high":
            case "minor-mem-high":
            case "minor-disk-error":
            case "minor-io-hang":
            case "minor-delay":
            case "minor-loss":
                num = nodes.size() % 2 == 0 ? nodes.size() / 2 - 1 : nodes.size() / 2;
                return faultInRandomNumberNodes(nodes, num, faultName);
            case "major-suspend":
            case "major-kill":
            case "major-cpu-high":
            case "major-mem-high":
            case "major-disk-error":
            case "major-io-hang":
            case "major-delay":
            case "major-loss":
                num = nodes.size() % 2 == 0 ? nodes.size() / 2 : nodes.size() / 2 + 1;
                return faultInRandomNumberNodes(nodes, num, faultName);
            case "random-suspend":
            case "random-kill":
            case "random-delay":
            case "random-loss":
            case "random-cpu-high":
            case "random-mem-high":
            case "random-disk-error":
            case "random-io-hang":
                num = random.nextInt(nodes.size()) + 1;
                return faultInRandomNumberNodes(nodes, num, faultName);
            case "random-partition":
                return randomPartition(nodes, faultName);
            case "partition-majorities-ring":
                return partitionMajoritiesRing(nodes, faultName);
            case "bridge":
                return bridge(nodes, faultName);
            default:
                throw new IllegalArgumentException("Fault cannot be recognized");
        }
    }

    public static List<FaultOperation> generate(Collection<String> nodes, Collection<String> faultNodes,
                                                String faultName) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Nodes cannot be null or empty");
        }
        List<FaultOperation> operations = new ArrayList<>();
        switch (faultName) {
            case "fixed-kill":
            case "fixed-suspend":
            case "fixed-cpu-high":
            case "fixed-mem-high":
            case "fixed-disk-error":
            case "fixed-io-hang":
            case "fixed-delay":
            case "fixed-loss":
                for (String node : faultNodes) {
                    operations.add(new FaultOperation(faultName, node));
                }
                break;
            case "fixed-partition":
                Set<String> partition1 = new HashSet<>(faultNodes);
                Set<String> partition2 = new HashSet<>(nodes);
                partition2.removeAll(partition1);
                partition1.forEach(node -> operations.add(getPartitionOperation(faultName, node, partition2)));
                break;
            default:
                throw new IllegalArgumentException("Fault cannot be recognized");
        }
        return operations;
    }

    public static List<FaultOperation> generate(Collection<String> nodes, String stateClass, String metaName, String metaNode,
                                                String faultName) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Nodes cannot be null or empty");
        }
        int num = 1;
        switch (faultName) {
            case "leader-partition":
                num = nodes.size() % 2 == 0 ? nodes.size() / 2 - 1 : nodes.size() / 2;
                return leaderPartition(nodes, metaName, stateClass, metaNode, faultName, num);
            case "leader-kill":
            case "leader-suspend":
                return faultInRandomNumberLeaderNodes(faultName, stateClass, num, metaName, metaNode);
            default:
                throw new IllegalArgumentException("Fault cannot be recognized");
        }
    }

    private static List<FaultOperation> leaderPartition(Collection<String> nodes, String clusterName, String stateClass, String addr, String faultName, int num) {
        List<FaultOperation> operations = new ArrayList<>();
        try {
            chaosState = (ChaosState) Class.forName(stateClass).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        chaosState.initialize(clusterName, addr);
        Set<String> leaderNode = null;
        leaderNode = chaosState.getLeader();

        List<String> shuffleNodes = new ArrayList<>(nodes);
        Collections.shuffle(shuffleNodes);
        Set<String> partition1 = new HashSet<>(leaderNode);
        Set<String> partition2 = new HashSet<>(nodes);
        int i = 0;
        while (partition1.size() < num) {
            partition1.add(shuffleNodes.get(i));
            i++;
        }
        partition2.removeAll(partition1);
        partition1.forEach(node -> operations.add(getPartitionOperation(faultName, node, partition2)));

        return operations;
    }

    private static List<FaultOperation> randomPartition(Collection<String> nodes, String faultName) {
        int num = random.nextInt(nodes.size() - 1) + 1;
        List<FaultOperation> operations = new ArrayList<>();
        List<String> shuffleNodes = new ArrayList<>(nodes);
        Collections.shuffle(shuffleNodes);
        Set<String> partition1 = new HashSet<>();
        Set<String> partition2 = new HashSet<>();
        for (int i = 0; i < shuffleNodes.size(); i++) {
            if (i < num) {
                partition1.add(shuffleNodes.get(i));
            } else {
                partition2.add(shuffleNodes.get(i));
            }
        }

        if (num <= shuffleNodes.size() - num) {
            partition1.forEach(node -> operations.add(getPartitionOperation(faultName, node, partition2)));
        } else {
            partition2.forEach(node -> operations.add(getPartitionOperation(faultName, node, partition1)));
        }

        return operations;
    }

    private static List<FaultOperation> partitionMajoritiesRing(Collection<String> nodes, String faultName) {
        if (nodes.size() <= 3)
            throw new IllegalArgumentException("The number of nodes less than or equal to 3, unable to form partition-majorities-ring");
        List<FaultOperation> operations = new ArrayList<>();
        List<String> shuffleNodes = new LinkedList<>(nodes);
        Collections.shuffle(shuffleNodes);
        for (int i = 0; i < shuffleNodes.size(); i++) {
            Set<String> partitionNodes = new HashSet<>(shuffleNodes);
            partitionNodes.remove(shuffleNodes.get(i));
            if (i == 0) {
                partitionNodes.remove(shuffleNodes.get(shuffleNodes.size() - 1));
                partitionNodes.remove(shuffleNodes.get(1));
            } else if (i == shuffleNodes.size() - 1) {
                partitionNodes.remove(shuffleNodes.get(shuffleNodes.size() - 2));
                partitionNodes.remove(shuffleNodes.get(0));
            } else {
                partitionNodes.remove(shuffleNodes.get(i - 1));
                partitionNodes.remove(shuffleNodes.get(i + 1));
            }
            operations.add(getPartitionOperation(faultName, shuffleNodes.get(i), partitionNodes));
        }
        return operations;
    }

    private static List<FaultOperation> bridge(Collection<String> nodes, String faultName) {
        if (nodes.size() != 5)
            throw new IllegalArgumentException("The number of nodes is not equal to 5, unable to form bridge");
        List<FaultOperation> operations = new ArrayList<>();
        List<String> shuffleNodes = new LinkedList<>(nodes);
        Collections.shuffle(shuffleNodes);
        Set<String> partitionSet1 = new HashSet<>();
        partitionSet1.add(shuffleNodes.get(0));
        partitionSet1.add(shuffleNodes.get(1));
        Set<String> partitionSet2 = new HashSet<>();
        partitionSet2.add(shuffleNodes.get(2));
        partitionSet2.add(shuffleNodes.get(3));
        for (int i = 0; i < shuffleNodes.size(); i++) {
            if (i == 4) {
                break;
            } else if (i < 2) {
                operations.add(getPartitionOperation(faultName, shuffleNodes.get(i), partitionSet2));
            }
//            else {
//                operations.add(getPartitionOperation(faultName, shuffleNodes.get(i), partitionSet1));
//            }
        }
        return operations;
    }

    private static List<FaultOperation> faultInRandomNumberNodes(Collection<String> nodes, int num, String faultName) {
        List<FaultOperation> operations = new ArrayList<>();
        List<String> shuffleNodes = new ArrayList<>(nodes);
        Collections.shuffle(shuffleNodes);
        for (int i = 0; i < num; i++) {
            FaultOperation operation = new FaultOperation(faultName, shuffleNodes.get(i));
            operations.add(operation);
        }
        return operations;
    }

    private static List<FaultOperation> faultInRandomNumberLeaderNodes(String faultName, String stateClass, int num,
                                                                       String metaName, String metaNode) {
        List<FaultOperation> operations = new ArrayList<>();
        try {
            chaosState = (QueueState) Class.forName(stateClass).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        chaosState.initialize(metaName, metaNode);
        Set<String> leaderNode;
        leaderNode = chaosState.getLeader();
        List<String> shuffleNodes = new ArrayList<>(leaderNode);
        Collections.shuffle(shuffleNodes);
        for (int i = 0; i < num; i++) {
            FaultOperation operation = new FaultOperation(faultName, shuffleNodes.get(i));
            operations.add(operation);
        }
        return operations;
    }

    private static FaultOperation getPartitionOperation(String faultName, String node, Set<String> partitionNodes) {
        List<String> invokeArgs = new ArrayList<>(partitionNodes);
        List<String> recoverArgs = new ArrayList<>(partitionNodes);
        return new FaultOperation(faultName, node, invokeArgs, recoverArgs);
    }
}
