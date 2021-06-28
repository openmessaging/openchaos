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

package io.openchaos.fault;

import io.openchaos.ChaosControl;
import io.openchaos.generator.FaultGenerator;
import io.openchaos.generator.FaultOperation;
import io.openchaos.recorder.FaultLogEntry;
import io.openchaos.recorder.Recorder;
import io.openchaos.common.utils.NetUtil;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The fault which create some network faults on particular node
 */
public class NetFault implements Fault {

    private static final Logger log = LoggerFactory.getLogger(ChaosControl.class);
    private volatile List<FaultOperation> faultOperations;
    private String mode;
    private String stateClass;
    private Set<String> nodes;
    private List<String> faultNodes;
    private String metaNode;
    private String metaName;
    private Recorder recorder;

    public NetFault(Set<String> nodes, String mode, Recorder recorder) {
        this.mode = mode;
        this.nodes = nodes;
        this.recorder = recorder;
    }

    public NetFault(Set<String> nodes, String mode, Recorder recorder, List<String> faultNodes) {
        this.mode = mode;
        this.nodes = nodes;
        this.recorder = recorder;
        this.faultNodes = faultNodes;
    }

    public NetFault(Set<String> nodes, String stateClass, String metaNode, String metaName,
                    String mode, Recorder recorder) {
        this.mode = mode;
        this.nodes = nodes;
        this.recorder = recorder;
        this.metaName = metaName;
        this.metaNode = metaNode;
        this.stateClass = stateClass;
    }

    @Override
    public synchronized void invoke() {
        log.info("Invoke {} fault", mode);

        if (faultNodes != null) {
            faultOperations = FaultGenerator.generate(nodes, faultNodes, mode);
        } else if (metaName == null) {
            faultOperations = FaultGenerator.generate(nodes, mode);
        } else {
            faultOperations = FaultGenerator.generate(nodes, stateClass, metaName, metaNode, mode);
        }
        recorder.recordFault(new FaultLogEntry(mode, "start", System.currentTimeMillis(), faultOperations == null ? null : faultOperations.toString()));
        for (FaultOperation operation : faultOperations) {
            log.info("Invoke node {} fault, fault is {}, invoke args is {}",
                operation.getNode(), operation.getName(), operation.getInvokeArgs());
            try {
                switch (operation.getName()) {
                    case "random-partition":
                    case "fixed-partition":
                    case "leader-partition":
                        for (String partitionNode : operation.getInvokeArgs()) {
                            NetUtil.partition(operation.getNode(), partitionNode);
                        }
                        break;
                    case "partition-majorities-ring":
                        if (nodes.size() <= 3)
                            throw new IllegalArgumentException("The number of nodes less than or equal to 3, unable to form partition-majorities-ring");
                        for (String partitionNode : operation.getInvokeArgs()) {
                            NetUtil.partition(operation.getNode(), partitionNode);
                        }
                        break;
                    case "bridge":
                        if (nodes.size() != 5)
                            throw new IllegalArgumentException("The number of nodes is not equal to 5, unable to form bridge");
                        for (String partitionNode : operation.getInvokeArgs()) {
                            NetUtil.partition(operation.getNode(), partitionNode);
                        }
                        break;
                    case "random-delay":
                        NetUtil.delay(operation.getNode());
                        break;
                    case "random-loss":
                        List<String> targetNodes = nodes.stream().filter(node -> !node.equals(operation.getNode())).collect(Collectors.toList());
                        NetUtil.loss(operation.getNode(), targetNodes);
                        break;
                    default:
                        log.error("No such fault");
                        break;
                }
            } catch (Exception e) {
                log.error("Invoke fault {} failed", operation.getName(), e);
            }
        }
    }

    @Override
    public synchronized void recover() {
        if (faultOperations == null)
            return;
        log.info("Recover {} fault", mode);
        for (FaultOperation operation : faultOperations) {
            try {
                log.info("Recover node {} fault, fault is {}, recover args is {}",
                    operation.getNode(), operation.getName(), operation.getRecoverArgs());
                switch (operation.getName()) {
                    case "random-partition":
                    case "fixed-partition":
                    case "leader-partiton":
                        NetUtil.healPartition(operation.getNode());
                        break;
                    case "partition-majorities-ring":
                        if (nodes.size() <= 3)
                            throw new IllegalArgumentException("Number of nodes less than or equal to 3, unable to form partition-majorities-ring");
                        NetUtil.healPartition(operation.getNode());
                        break;
                    case "bridge":
                        if (nodes.size() != 5)
                            throw new IllegalArgumentException("Number of nodes is not equal to 5, unable to form bridge");
                        NetUtil.healPartition(operation.getNode());
                        break;
                    case "random-delay":
                        NetUtil.healDelay(operation.getNode());
                        break;
                    case "random-loss":
                        NetUtil.healLoss(operation.getNode());
                        break;
                    default:
                        log.error("No such fault");
                        break;
                }
            } catch (Exception e) {
                log.error("Recover fault {} failed", operation.getName(), e);
            }
        }
        recorder.recordFault(new FaultLogEntry(mode, "end", System.currentTimeMillis(), faultOperations == null ? null : faultOperations.toString()));
        faultOperations = null;
    }
}
