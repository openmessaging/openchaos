/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.openmessaging.chaos.fault;

import io.openmessaging.chaos.ChaosControl;
import io.openmessaging.chaos.generator.FaultOperation;
import io.openmessaging.chaos.generator.NetFaultGenerator;
import io.openmessaging.chaos.utils.NetUtil;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The fault which create some network faults on particular node
 */
public class NetFault implements Fault {

    private volatile List<FaultOperation> faultOperations;

    private NetFaultGenerator netFaultGenerator;

    private static final Logger logger = LoggerFactory.getLogger(ChaosControl.class);

    private String mode;

    private List<String> nodes;

    public NetFault(List<String> nodes, String mode) {
        this.mode = mode;
        this.nodes = nodes;
        this.netFaultGenerator = new NetFaultGenerator(nodes, mode);
    }

    @Override public synchronized void invoke() {
        logger.info("invoke {} fault", mode);
        faultOperations = netFaultGenerator.generate();
        for (FaultOperation operation : faultOperations) {
            logger.info("invoke node {} fault, fault is {}, invoke args is {}",
                operation.getNode(), operation.getName(), operation.getInvokeArgs());
            try {
                switch (operation.getName()) {
                    case "random-partition":
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
                        logger.error("no such fault");
                        break;
                }
            } catch (Exception e) {
                logger.error("invoke fault {} failed", operation.getName());
            }
        }
    }

    @Override public synchronized void recover() {
        if (faultOperations == null)
            return;
        logger.info("recover {} fault", mode);
        for (FaultOperation operation : faultOperations) {
            try {
                logger.info("recover node {} fault, fault is {}, recover args is {}",
                    operation.getNode(), operation.getName(), operation.getRecoverArgs());
                switch (operation.getName()) {
                    case "random-partition":
                        NetUtil.healPartition(operation.getNode());
                        break;
                    case "random-delay":
                        NetUtil.healDelay(operation.getNode());
                        break;
                    case "random-loss":
                        NetUtil.healLoss(operation.getNode());
                        break;
                    default:
                        logger.error("no such fault");
                        break;
                }
            } catch (Exception e) {
                logger.error("recover fault {} failed", operation.getName());
            }
        }
        faultOperations = null;
    }
}
