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

package io.openmessaging.chaos.fault;

import io.openmessaging.chaos.ChaosControl;
import io.openmessaging.chaos.common.utils.NetUtil;
import io.openmessaging.chaos.generator.FaultGenerator;
import io.openmessaging.chaos.generator.FaultOperation;
import io.openmessaging.chaos.generator.FixedFaultGenerator;
import io.openmessaging.chaos.generator.NetFaultGenerator;
import io.openmessaging.chaos.recorder.Recorder;
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
    private FaultGenerator faultGenerator;
    private String mode;

    private List<String> nodes;

    private Recorder recorder;

    public NetFault(List<String> nodes, String mode, Recorder recorder) {
        this.mode = mode;
        this.nodes = nodes;
        this.recorder = recorder;
        this.faultGenerator = new NetFaultGenerator(nodes, mode);
    }

    public NetFault(List<String> nodes, String mode, Recorder recorder, List<String> faultNodes) {
        this.mode = mode;
        this.nodes = nodes;
        this.recorder = recorder;
        this.faultGenerator = new FixedFaultGenerator(nodes, faultNodes, mode);
    }

    @Override
    public synchronized void invoke() {
        log.info("Invoke {} fault", mode);
        recorder.recordFaultStart(mode, System.currentTimeMillis());
        faultOperations = faultGenerator.generate();
        for (FaultOperation operation : faultOperations) {
            log.info("Invoke node {} fault, fault is {}, invoke args is {}",
                operation.getNode(), operation.getName(), operation.getInvokeArgs());
            try {
                switch (operation.getName()) {
                    case "random-partition":
                    case "fixed-partition":
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
        recorder.recordFaultEnd(mode, System.currentTimeMillis());
        for (FaultOperation operation : faultOperations) {
            try {
                log.info("Recover node {} fault, fault is {}, recover args is {}",
                    operation.getNode(), operation.getName(), operation.getRecoverArgs());
                switch (operation.getName()) {
                    case "random-partition":
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
        faultOperations = null;
    }
}
