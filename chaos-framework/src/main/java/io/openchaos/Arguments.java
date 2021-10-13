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

package io.openchaos;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;

public class Arguments {

    @Parameter(names = {"-h", "--help"}, description = "Help message", help = true)
    public boolean help;

    @Parameter(names = {
        "-d",
        "--driver"}, description = "Driver. eg: driver-rocketmq/rocketmq.yaml", required = true)
    public String driver;

    @Parameter(names = {
        "-t",
        "--limit-time"}, description = "Chaos execution time in seconds (excluding check time and recovery time). eg: 60", validateWith = PositiveInteger.class)
    public int time = 60;

    @Parameter(names = {
        "-c",
        "--concurrency"}, description = "The number of clients. eg: 5", validateWith = PositiveInteger.class)
    public int concurrency = 4;

    @Parameter(names = {
        "-r",
        "--rate"}, description = "Approximate number of requests per second. eg: 20", validateWith = PositiveInteger.class)
    public int rate = 20;

    @Parameter(names = {
        "-u",
        "--username"}, description = "User name for ssh remote login. eg: admin")
    public String username = "root";

    @Parameter(names = {
        "-f",
        "--fault"}, description = "Fault type to be injected. eg: noop, minor-kill, major-kill, random-kill, fixed-kill, random-partition, " +
        "fixed-partition, partition-majorities-ring, bridge, random-loss, minor-suspend, major-suspend, random-suspend, fixed-suspend"
        , validateWith = FaultValidator.class)
    public String fault = "noop";

    @Parameter(names = {
        "-n",
        "--fault-nodes"}, description = "The nodes need to be fault injection. The nodes are separated by semicolons. eg: 'n1;n2;n3' " +
        " Note: this parameter must be used with fixed-xxx faults such as fixed-kill, fixed-partition, fixed-suspend.")
    public String faultNodes = null;

    @Parameter(names = {
        "-i",
        "--fault-interval"}, description = "Fault injection interval (The unit is second). eg: 30", validateWith = PositiveInteger.class)
    public int interval = 30;

    @Parameter(names = {
        "-m",
        "--model"}, description = "Test model. Currently queue model and kv model are supported.")
    public String model = "queue";

    @Parameter(names = {
        "--rto"}, description = "Calculate failure recovery time in fault.")
    public boolean rto = false;

    @Parameter(names = {
        "--recovery"}, description = "Calculate failure recovery time.")
    public boolean recovery = false;

    /**
     * This property is deprecated and invalid. You can set this property in the driver file instead
     */
    @Deprecated
    @Parameter(names = {
        "--order"}, description = "Check the partition order of messaging platform. Just for mq model.")
    public boolean isOrderTest = false;

    /**
     * This property is deprecated and invalid. You can set this property in the driver file instead
     */
    @Deprecated
    @Parameter(names = {
        "--pull"}, description = "Driver use pull consumer, default is push consumer. Just for mq model.")
    public boolean pull = false;

    @Parameter(names = {
        "--install"}, description = "Whether to install program. It will download the installation package on each cluster node. " +
        "When you first use Open-Chaos to test a distributed system, it should be true.")
    public boolean install = false;

    @Parameter(names = {
        "--restart"}, description = "Whether to restart program. If you want the node to be restarted, and " +
        "shut down after the experiment, it should be true. ")
    public boolean restart = false;
    
    @Parameter(names = {
        "--agent"}, description = "Run program as a http agent.")
    public boolean agent = false;

    @Parameter(names = {
        "-p",
        "--port"}, description = "The listening port of http agent.")
    public int port = 8080;

    @Parameter(names = {
        "--output-dir"
        }, description = "The directory of history files and the output files")
    public String outputDir;

    @Parameter(names = {
        "--recovery-time"
        }, description = "Recovery time after stop (the unit is second).")
    public long recoveryTime  = 30;
}
