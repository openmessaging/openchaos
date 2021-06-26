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

package io.openchaos.fault;

import io.openchaos.ChaosControl;
import io.openchaos.driver.ChaosNode;
import io.openchaos.generator.FaultGenerator;
import io.openchaos.recorder.FaultLogEntry;
import io.openchaos.recorder.Recorder;
import io.openchaos.generator.FaultOperation;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The fault which kill the process on particular node
 */
public class KillFault implements Fault {

    private static final Logger log = LoggerFactory.getLogger(ChaosControl.class);
    private volatile List<FaultOperation> faultOperations;
    private Map<String, ChaosNode> nodesMap;
    private List<String> faultNodes;
    private String mode;
    private Recorder recorder;
    private String metaNode;
    private String metaName;
    private String stateClass;

    public KillFault(Map<String, ChaosNode> nodesMap, String mode, Recorder recorder) {
        this.nodesMap = nodesMap;
        this.mode = mode;
        this.recorder = recorder;
    }

    public KillFault(Map<String, ChaosNode> nodesMap, String mode, Recorder recorder, List<String> faultNodes) {
        this.nodesMap = nodesMap;
        this.mode = mode;
        this.recorder = recorder;
        this.faultNodes = faultNodes;
    }

    public KillFault(Map<String, ChaosNode> nodesMap, String stateClass, String metaNode,
                     String metaName, String mode, Recorder recorder) {
        this.nodesMap = nodesMap;
        this.mode = mode;
        this.recorder = recorder;
        this.metaName = metaName;
        this.metaNode = metaNode;
        this.stateClass = stateClass;
    }

    @Override
    public synchronized void invoke() {
        log.info("Invoke {} fault....", mode);

        if (faultNodes != null) {
            faultOperations = FaultGenerator.generate(nodesMap.keySet(), faultNodes, mode);
        } else if (metaName == null) {
            faultOperations = FaultGenerator.generate(nodesMap.keySet(), mode);
        } else {
            faultOperations = FaultGenerator.generate(nodesMap.keySet(), stateClass, metaName, metaNode, mode);
        }
        recorder.recordFault(new FaultLogEntry(mode, "start", System.currentTimeMillis(), faultOperations == null ? null : faultOperations.toString()));
        for (FaultOperation operation : faultOperations) {
            log.info("Kill node {} processes...", operation.getNode());
            ChaosNode chaosNode = nodesMap.get(operation.getNode());
            chaosNode.kill();
        }
    }

    @Override
    public synchronized void recover() {
        if (faultOperations == null)
            return;
        log.info("Recover {} fault....", mode);
        for (FaultOperation operation : faultOperations) {
            log.info("Restart node {} processes...", operation.getNode());
            ChaosNode chaosNode = nodesMap.get(operation.getNode());
            chaosNode.start();
        }
        recorder.recordFault(new FaultLogEntry(mode, "end", System.currentTimeMillis(), faultOperations == null ? null : faultOperations.toString()));
        faultOperations = null;
    }
}
