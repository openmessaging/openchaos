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
import io.openmessaging.chaos.driver.ChaosNode;
import io.openmessaging.chaos.generator.FaultGenerator;
import io.openmessaging.chaos.generator.FaultOperation;
import io.openmessaging.chaos.recorder.FaultLogEntry;
import io.openmessaging.chaos.recorder.Recorder;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PauseFault implements Fault {
    private static final Logger log = LoggerFactory.getLogger(ChaosControl.class);
    private volatile List<FaultOperation> faultOperations;
    private Map<String, ChaosNode> nodesMap;
    private List<String> faultNodes;
    private String mode;
    private Recorder recorder;

    public PauseFault(Map<String, ChaosNode> nodesMap, String mode, Recorder recorder) {
        this.nodesMap = nodesMap;
        this.mode = mode;
        this.recorder = recorder;
    }

    public PauseFault(Map<String, ChaosNode> nodesMap, String mode, Recorder recorder, List<String> faultNodes) {
        this.nodesMap = nodesMap;
        this.mode = mode;
        this.recorder = recorder;
        this.faultNodes = faultNodes;
    }

    @Override
    public synchronized void invoke() {
        log.info("Invoke {} fault....", mode);
        if (faultNodes != null) {
            faultOperations = FaultGenerator.generate(nodesMap.keySet(), faultNodes, mode);
        } else {
            faultOperations = FaultGenerator.generate(nodesMap.keySet(), mode);
        }
        recorder.recordFault(new FaultLogEntry(mode,"start", System.currentTimeMillis(), faultOperations == null ? null : faultOperations.toString()));
        for (FaultOperation operation : faultOperations) {
            log.info("Suspend node {} processes...", operation.getNode());
            ChaosNode chaosNode = nodesMap.get(operation.getNode());
            chaosNode.pause();
        }
    }

    @Override
    public synchronized void recover() {
        if (faultOperations == null)
            return;
        log.info("Recover {} fault....", mode);
        for (FaultOperation operation : faultOperations) {
            log.info("Recovery node {} processes...", operation.getNode());
            ChaosNode chaosNode = nodesMap.get(operation.getNode());
            chaosNode.resume();
        }
        recorder.recordFault(new FaultLogEntry(mode,"end", System.currentTimeMillis(), faultOperations == null ? null : faultOperations.toString()));
        faultOperations = null;
    }
}
