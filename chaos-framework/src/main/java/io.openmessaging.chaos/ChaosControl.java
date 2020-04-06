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

package io.openmessaging.chaos;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.validators.PositiveInteger;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.util.concurrent.RateLimiter;
import io.openmessaging.chaos.checker.Checker;
import io.openmessaging.chaos.checker.MQChecker;
import io.openmessaging.chaos.checker.OrderChecker;
import io.openmessaging.chaos.checker.PerfChecker;
import io.openmessaging.chaos.checker.RTOChecker;
import io.openmessaging.chaos.checker.result.TestResult;
import io.openmessaging.chaos.common.utils.SshUtil;
import io.openmessaging.chaos.driver.MQChaosNode;
import io.openmessaging.chaos.fault.Fault;
import io.openmessaging.chaos.fault.KillFault;
import io.openmessaging.chaos.fault.NetFault;
import io.openmessaging.chaos.fault.NoopFault;
import io.openmessaging.chaos.fault.SuspendFault;
import io.openmessaging.chaos.model.Model;
import io.openmessaging.chaos.model.QueueModel;
import io.openmessaging.chaos.recorder.Recorder;
import io.openmessaging.chaos.worker.FaultWorker;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaosControl {

    static class Arguments {

        @Parameter(names = {"-h", "--help"}, description = "Help message", help = true)
        boolean help;

        @Parameter(names = {
            "-d",
            "--drivers"}, description = "Drivers list. eg.: driver-rocketmq/rocketmq.yaml", required = true)
        List<String> drivers;

        @Parameter(names = {
            "-t",
            "--limit-time"}, description = "Chaos execution time in seconds (excluding check time and recovery time). eg: 60", validateWith = PositiveInteger.class)
        int time = 60;

        @Parameter(names = {
            "-c",
            "--concurrency"}, description = "The number of clients. eg: 5", validateWith = PositiveInteger.class)
        int concurrency = 4;

        @Parameter(names = {
            "-r",
            "--rate"
        }, description = "Approximate number of requests per second. eg: 20", validateWith = PositiveInteger.class)
        int rate = 20;

        @Parameter(names = {
            "-u",
            "--username"
        }, description = "User name for ssh remote login. eg: admin")
        String username = "root";

        @Parameter(names = {
            "-f",
            "--fault"
        }, description = "Fault type to be injected. eg: noop, minor-kill, major-kill, random-kill, random-partition, " +
            "partition-majorities-ring, bridge, random-loss, minor-suspend, major-suspend, random-suspend"
            , validateWith = FaultValidator.class)
        String fault = "noop";

        @Parameter(names = {
            "-i",
            "--fault-interval"}, description = "Fault execution interval. eg: 30", validateWith = PositiveInteger.class)
        int interval = 30;

        @Parameter(names = {
            "--rto"}, description = "Calculate failure recovery time.")
        boolean rto = false;

        @Parameter(names = {
            "--order-test"}, description = "Turn on order test.")
        boolean isOrderTest = false;

        @Parameter(names = {
            "--install"}, description = "Whether to install program. It will download the installation package on each cluster node. " +
            "When you first use openmessaging-chaos to test a distributed system, it should be true.")
        boolean install = false;
    }

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static {
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    private static final Logger logger = LoggerFactory.getLogger(ChaosControl.class);

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

        arguments.drivers.forEach(driverConfig -> {

            Model model = null;
            Recorder recorder = null;
            List<TestResult> resultList = new ArrayList<>();

            try {

                File driverConfigFile = new File(driverConfig);
                DriverConfiguration driverConfiguration = mapper.readValue(driverConfigFile,
                    DriverConfiguration.class);

                SshUtil.init(arguments.username, driverConfiguration.nodes);

                logger.info("--------------- CHAOS TEST --- DRIVER : {}---------------", driverConfiguration.name);

                String historyFile = String.format("%s-%s-chaos-history-file", dateFormat.format(new Date()), driverConfiguration.name);

                RateLimiter rateLimiter = RateLimiter.create(arguments.rate);

                recorder = Recorder.newRecorder(historyFile);

                if (recorder == null) {
                    System.err.printf("Create %s failed", historyFile);
                    System.exit(-1);
                }
                List<String> shardingKeys = new ArrayList<>();
                for (int i = 0; i < 2 * arguments.concurrency; i++) {
                    shardingKeys.add("shardingKey" + i);
                }

                //Currently only queue model is supported
                model = new QueueModel(arguments.concurrency, rateLimiter, recorder, driverConfigFile);

                Map<String, MQChaosNode> map = null;

                if (driverConfiguration.nodes != null && !driverConfiguration.nodes.isEmpty()) {
                    map = model.setupCluster(driverConfiguration.nodes, arguments.install);
                }

                model.setupClient(arguments.isOrderTest, shardingKeys);

                //Initial fault
                Fault fault;
                if (map == null || map.isEmpty()) {
                    logger.warn("Configure file does not contain nodes, use noop fault");
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
                        case "random-partition":
                        case "random-delay":
                        case "random-loss":
                            fault = new NetFault(driverConfiguration.nodes, arguments.fault, recorder);
                            break;
                        case "partition-majorities-ring":
                            if (driverConfiguration.nodes.size() <= 3)
                                throw new IllegalArgumentException("Number of nodes less than or equal to 3, unable to form partition-majorities-ring");
                            fault = new NetFault(driverConfiguration.nodes, arguments.fault, recorder);
                            break;
                        case "bridge":
                            if (driverConfiguration.nodes.size() != 5)
                                throw new IllegalArgumentException("Number of nodes is not equal to 5, unable to form bridge");
                            fault = new NetFault(driverConfiguration.nodes, arguments.fault, recorder);
                            break;
                        case "minor-suspend":
                        case "major-suspend":
                        case "random-suspend":
                            fault = new SuspendFault(map, arguments.fault, recorder);
                            break;
                        default:
                            throw new RuntimeException("no such fault");
                    }
                }

                //Start fault worker
                FaultWorker faultWorker = new FaultWorker(logger, fault, arguments.interval);

                faultWorker.start();

                long testStartTimeStamp = System.currentTimeMillis();

                //Start model
                model.start();

                //Wait for the chaos test to execute
                Thread.sleep(TimeUnit.SECONDS.toMillis(arguments.time));

                //Stop model
                model.stop();

                //Interrupt fault worker
                faultWorker.breakLoop();

                //Recovery fault
                fault.recover();

                logger.info("Wait for recovery some time");

                //Wait for recovery, sleep 20 s
                Thread.sleep(TimeUnit.SECONDS.toMillis(20));

                //Model do something after stop
                model.afterStop();

                recorder.flush();

                long testEndTimestamp = System.currentTimeMillis();

                logger.info("Start check...");

                List<Checker> checkerList = new ArrayList<>();

                checkerList.add(new MQChecker(historyFile));
                checkerList.add(new PerfChecker(historyFile, testStartTimeStamp, testEndTimestamp));
                if(arguments.rto){
                    checkerList.add(new RTOChecker(historyFile));
                }
                if (arguments.isOrderTest) {
                    checkerList.add(new OrderChecker(historyFile, shardingKeys));
                }
                checkerList.forEach(checker -> resultList.add(checker.check()));

                logger.info("Check complete.");

            } catch (Exception e) {
                logger.error("Failed to run chaos test.", e);

                if (recorder != null) {
                    recorder.delete();
                    recorder = null;
                }
            } finally {
                if (model != null)
                    model.shutdown();

                if (recorder != null)
                    recorder.close();

                SshUtil.close();
            }

            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(3));
            } catch (InterruptedException e) {
                logger.error("", e);
            }

            if (resultList.size() != 0) {
                logger.info("----CHAOS TEST RESULT----");
                resultList.forEach(testResult -> logger.info(testResult.toString()));
                logger.info("----CHAOS TEST RESULT----");
            }
        });
    }

}
