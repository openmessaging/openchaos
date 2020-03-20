package io.openmessaging.chaos.fault;

import io.openmessaging.chaos.ChaosControl;
import io.openmessaging.chaos.driver.MQChaosNode;
import io.openmessaging.chaos.generator.FaultOperation;
import io.openmessaging.chaos.generator.SingleFaultGenerator;
import io.openmessaging.chaos.recorder.Recorder;
import io.openmessaging.chaos.utils.SuspendProcessUtil;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuspendFault implements Fault {
    private volatile List<FaultOperation> faultOperations;

    private Map<String, MQChaosNode> nodesMap;

    private SingleFaultGenerator singleFaultGenerator;

    private String mode;

    private Recorder recorder;

    private static final Logger logger = LoggerFactory.getLogger(ChaosControl.class);

    public SuspendFault(Map<String, MQChaosNode> nodesMap, String mode, Recorder recorder) {
        this.nodesMap = nodesMap;
        this.mode = mode;
        this.recorder = recorder;
        this.singleFaultGenerator = new SingleFaultGenerator(nodesMap.keySet(), mode);
    }

    @Override public synchronized void invoke() {
        logger.info("Invoke {} fault....", mode);
        recorder.recordFaultStart(mode, System.currentTimeMillis());
        faultOperations = singleFaultGenerator.generate();
        for (FaultOperation operation : faultOperations) {
            logger.info("Suspend node {} processes...", operation.getNode());
            MQChaosNode mqChaosNode = nodesMap.get(operation.getNode());
            try {
                SuspendProcessUtil.suspend(operation.getNode(), mqChaosNode.getSuspendProcessName());
            } catch (Exception e) {
                logger.error("Invoke fault {} failed", operation.getName(), e);
            }
        }
    }

    @Override public synchronized void recover() {
        if (faultOperations == null)
            return;
        logger.info("Recover {} fault....", mode);
        recorder.recordFaultEnd(mode, System.currentTimeMillis());
        for (FaultOperation operation : faultOperations) {
            logger.info("Recovery node {} processes...", operation.getNode());
            MQChaosNode mqChaosNode = nodesMap.get(operation.getNode());
            try {
                SuspendProcessUtil.recover(operation.getNode(), mqChaosNode.getSuspendProcessName());
            } catch (Exception e) {
                logger.error("Recovery fault {} failed", operation.getName(), e);
            }

        }
        faultOperations = null;
    }
}
