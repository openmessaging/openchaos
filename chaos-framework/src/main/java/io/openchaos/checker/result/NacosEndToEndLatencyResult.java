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
package io.openchaos.checker.result;

public class NacosEndToEndLatencyResult extends TestResult {
    public String e2eTotalLatency;
    public String e2eAveLatency;
    public String e2e50Latency;
    public String e2e75Latency;
    public String e2e85Latency;
    public String e2e90Latency;
    public String e2e95Latency;
    public String minLatency;
    public String maxLatency;
    public String timeOutCount;
    public NacosEndToEndLatencyResult() {
        super("NacosEndToEndLatencyResult");
    }

}
