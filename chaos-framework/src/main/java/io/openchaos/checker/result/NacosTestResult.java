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

import java.util.Map;
import java.util.Set;


public class NacosTestResult extends TestResult {
    public long putInvokeCount;
    public long pubSuccessCount;
    public long subSuccessCount;
    public long subTimeOutCount;
    public Set<String> subTimeOutValues;
    public long lostValueCount = 0;
    public Set<String> lostValues;
    public boolean lineConsistent = false;
    public long unOrderCount;
    public Map<String,Long> unOrderValues;
    public long missValueCount;
    public Set<String> missValues;
    public boolean finalConsistent = false;

    public NacosTestResult() {
        super("NacosTestResult");
    }
}
