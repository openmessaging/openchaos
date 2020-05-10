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

package io.openmessaging.chaos;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.util.concurrent.RateLimiter;
import io.openmessaging.chaos.checker.Checker;
import io.openmessaging.chaos.checker.MQChecker;
import io.openmessaging.chaos.checker.OrderChecker;
import io.openmessaging.chaos.checker.PerfChecker;
import io.openmessaging.chaos.checker.RTOChecker;
import io.openmessaging.chaos.checker.RecoveryChecker;
import io.openmessaging.chaos.checker.result.TestResult;
import io.openmessaging.chaos.common.utils.SshUtil;
import io.openmessaging.chaos.driver.mq.MQChaosNode;
import io.openmessaging.chaos.fault.Fault;
import io.openmessaging.chaos.fault.KillFault;
import io.openmessaging.chaos.fault.NetFault;
import io.openmessaging.chaos.fault.NoopFault;
import io.openmessaging.chaos.fault.PauseFault;
import io.openmessaging.chaos.http.Agent;
import io.openmessaging.chaos.model.Model;
import io.openmessaging.chaos.model.QueueModel;
import io.openmessaging.chaos.recorder.Recorder;
import io.openmessaging.chaos.worker.FaultWorker;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaosControl {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final Logger log = LoggerFactory.getLogger(ChaosControl.class);

    private static volatile Status status = Status.COMPLETE;

    private static List<TestResult> resultList;

    private static Fault fault;

    private static Model model;

    private static Recorder recorder;

    private static FaultWorker faultWorker;

    private static long testStartTimeStamp;

    private static long testEndTimestamp;

    private static String historyFile;

    private static List<String> shardingKeys;

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    public static void main(String[] args) {
        final Arguments arguments = new Arguments();
        JCommander jc = new JCommander(arguments);
        jc.setProgramName("messaging-chaos");

        try {
            jc.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jc.usage();
            System.exit(-1);
        }

        if (arguments.help) {
            jc.usage();
            System.exit(-1);
        }

        try {

            if (arguments.agent) {

                log.info("Start ChaosControl HTTP Agent...");

                Agent.startAgent(arguments.port);

            } else {

                ready(arguments);

                if (ChaosControl.status == Status.READY_FAILED) {
                    return;
                }

                run(arguments);

                Timer timer = new Timer(true);
                timer.schedule(new TimerTask() {
                    @Override public void run() {
                        ChaosControl.status = Status.STOP_ING;
                    }
                }, arguments.time * 1000);

                while (ChaosControl.status != Status.STOP_ING) {
                    Thread.sleep(10);
                }

                stop();
                check(arguments);
                showResult();
            }

        } catch (Exception e) {
            log.error("Failed to run chaos test.", e);
            clearAfterException();
        } finally {
            clear();
        }
    }

    public static void ready(Arguments arguments) {

        ChaosControl.status = Status.READY_ING;

        try {

            if (arguments.driver == null || arguments.driver.isEmpty()) {
                throw new IllegalArgumentException("Parameter driver cannot be null or empty");
            }

            if (arguments.time <= 0) {
                throw new IllegalArgumentException("Parameter time should be positive");
            }

            if (arguments.concurrency <= 0) {
                throw new IllegalArgumentException("Parameter concurrency should be positive");
            }

            if (arguments.rate <= 0) {
                throw new IllegalArgumentException("Parameter rate should be positive");
            }

            File driverConfigFile = new File(arguments.driver);
            DriverConfiguration driverConfiguration = MAPPER.readValue(driverConfigFile,
                DriverConfiguration.class);

            List<String> faultNodeList = new ArrayList<>();
            if (arguments.fault.startsWith("fixed-")) {
                if (arguments.faultNodes == null || arguments.faultNodes.isEmpty()) {
                    throw new IllegalArgumentException("fault-nodes parameter can not be null or empty when inject fixed-xxx fault to system.");
                } else if (driverConfiguration.nodes == null || driverConfiguration.nodes.isEmpty()) {
                    throw new IllegalArgumentException("the nodes in configure file can not be null or empty when inject fixed-xxx fault to system.");
                } else {
                    String[] faultNodeArray = arguments.faultNodes.split(";");
                    for (String faultNode : faultNodeArray) {
                        if (!driverConfiguration.nodes.contains(faultNode)) {
                            throw new IllegalArgumentException(String.format("fault-node %s is not in current config file.", faultNode));
                        }
                    }
                    faultNodeList.addAll(Arrays.asList(faultNodeArray));
                }
            }

            SshUtil.init(arguments.username, driverConfiguration.nodes);

            log.info("--------------- CHAOS TEST --- DRIVER : {}---------------", driverConfiguration.name);

            historyFile = String.format("%s-%s-chaos-history-file", DATE_FORMAT.format(new Date()), driverConfiguration.name);

            RateLimiter rateLimiter = RateLimiter.create(arguments.rate);

            recorder = Recorder.newRecorder(historyFile);

            if (recorder == null) {
                System.err.printf("Create %s failed", historyFile);
                System.exit(-1);
            }
            shardingKeys = new ArrayList<>();
            for (int i = 0; i < 2 * arguments.concurrency; i++) {
                shardingKeys.add("shardingKey" + i);
            }

            //Currently only queue model is supported
            switch (arguments.model) {
                case "queue":
                    model = new QueueModel(arguments.concurrency, rateLimiter, recorder, driverConfigFile, arguments.isOrderTest, arguments.pull, shardingKeys);
                    break;
                default:
                    throw new RuntimeException("model not recognized.");
            }

            Map<String, MQChaosNode> map = null;

            if (driverConfiguration.nodes != null && !driverConfiguration.nodes.isEmpty()) {
                map = model.setupCluster(driverConfiguration.nodes, arguments.install);
            }

            model.setupClient();

            //Initial fault

            if (map == null || map.isEmpty()) {
                log.warn("Configure file does not contain nodes, use noop fault");
                fault = new NoopFault();
            } else {
                switch (arguments.fault) {
                    case "noop":
                        fault = new NoopFault();
                        break;
                    case "minor-kill":
                    case "major-kill":
                    case "random-kill":
                        fault = new KillFault(map, arguments.fault, recorder);
                        break;
                    case "fixed-kill":
                        fault = new KillFault(map, arguments.fault, recorder, faultNodeList);
                        break;
                    case "random-partition":
                    case "random-delay":
                    case "random-loss":
                        fault = new NetFault(driverConfiguration.nodes, arguments.fault, recorder);
                        break;
                    case "fixed-partition":
                        fault = new NetFault(driverConfiguration.nodes, arguments.fault, recorder, faultNodeList);
                        break;
                    case "partition-majorities-ring":
                        if (driverConfiguration.nodes.size() <= 3)
                            throw new IllegalArgumentException("The number of nodes less than or equal to 3, unable to form partition-majorities-ring");
                        fault = new NetFault(driverConfiguration.nodes, arguments.fault, recorder);
                        break;
                    case "bridge":
                        if (driverConfiguration.nodes.size() != 5)
                            throw new IllegalArgumentException("The number of nodes is not equal to 5, unable to form bridge");
                        fault = new NetFault(driverConfiguration.nodes, arguments.fault, recorder);
                        break;
                    case "minor-suspend":
                    case "major-suspend":
                    case "random-suspend":
                        fault = new PauseFault(map, arguments.fault, recorder);
                        break;
                    case "fixed-suspend":
                        fault = new PauseFault(map, arguments.fault, recorder, faultNodeList);
                        break;
                    default:
                        throw new RuntimeException("no such fault");
                }
            }

            //Ensure cluster and clients are ready
            log.info("Probe cluster, ensure cluster and clients are ready...");
            boolean isReady = model.probeCluster();
            if (!isReady) {
                throw new RuntimeException("Cluster and clients are not ready, please check.");
            }
            log.info("Cluster and clients are ready.");

            ChaosControl.status = Status.READY_COMPLETE;

        } catch (Exception e) {

            ChaosControl.status = Status.READY_FAILED;
            log.error("Failed to ready chaos test.", e);
            clearAfterException();
        }
    }

    public static void run(Arguments arguments) {

        ChaosControl.status = Status.RUN_ING;
        //Start fault worker
        faultWorker = new FaultWorker(log, fault, arguments.interval);

        faultWorker.start();

        testStartTimeStamp = System.currentTimeMillis();

        //Start model
        model.start();

    }

    public static void stop() {

        ChaosControl.status = Status.STOP_ING;
        //Stop model
        model.stop();

        //Interrupt fault worker
        faultWorker.breakLoop();

        //Recovery fault
        fault.recover();

        log.info("Wait for recovery some time");

        try {
            //Wait for recovery, sleep 20 s
            Thread.sleep(TimeUnit.SECONDS.toMillis(20));
        } catch (Exception e) {
            log.info("", e);
        }

        //Model do something after stop
        model.afterStop();

        recorder.flush();

        testEndTimestamp = System.currentTimeMillis();

    }

    public static void check(Arguments arguments) {

        ChaosControl.status = Status.CHECK_ING;

        log.info("Start check...");
        ChaosControl.status = Status.CHECK_ING;

        List<Checker> checkerList = new ArrayList<>();

        checkerList.add(new MQChecker(historyFile));
        checkerList.add(new PerfChecker(historyFile, testStartTimeStamp, testEndTimestamp));
        if (arguments.rto) {
            checkerList.add(new RTOChecker(historyFile));
        }
        if (arguments.recovery) {
            checkerList.add(new RecoveryChecker(historyFile));
        }
        if (arguments.isOrderTest) {
            checkerList.add(new OrderChecker(historyFile, shardingKeys));
        }

        resultList = new ArrayList<>();
        checkerList.forEach(checker -> resultList.add(checker.check()));

        log.info("Check complete.");

        ChaosControl.status = Status.COMPLETE;
    }

    private static void showResult() {

        if (resultList.size() != 0) {
            log.info("----CHAOS TEST RESULT----");
            resultList.forEach(testResult -> log.info(testResult.toString()));
            log.info("----CHAOS TEST RESULT----");
        }
    }

    private static void clearAfterException() {

        if (recorder != null) {
            recorder.delete();
            recorder.close();
            recorder = null;
        }

        if (model != null) {
            model.shutdown();
            model = null;
        }

        SshUtil.close();
    }

    public static void setStatus(Status status) {
        ChaosControl.status = status;
    }

    public static Status getStatus() {
        return status;
    }

    public static void setResultList(List<TestResult> resultList) {
        ChaosControl.resultList = resultList;
    }

    public static List<TestResult> getResultList() {
        return resultList;
    }

    public static Recorder getRecorder() {
        return recorder;
    }

    public static void clear() {

        if (recorder != null) {
            recorder.close();
            recorder = null;
        }

        if (model != null) {
            model.stop();
            model.shutdown();
            model = null;
        }

        fault = null;
        faultWorker = null;
        testStartTimeStamp = 0;
        testEndTimestamp = 0;
        historyFile = null;
        shardingKeys = null;

        SshUtil.close();
    }

    public enum Status {
        READY_ING,
        READY_COMPLETE,
        READY_FAILED,
        RUN_ING,
        STOP_ING,
        CHECK_ING,
        COMPLETE,
    }

}
