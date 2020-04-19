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

package io.openmessaging.chaos.generator;

import java.util.List;

public class FaultOperation {
    private String name;
    private String node;
    private List<String> invokeArgs;
    private List<String> recoverArgs;

    public FaultOperation(String name, String node) {
        this.name = name;
        this.node = node;
    }

    public FaultOperation(String name, String node, List<String> invokeArgs, List<String> recoverArgs) {
        this.name = name;
        this.node = node;
        this.invokeArgs = invokeArgs;
        this.recoverArgs = recoverArgs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public List<String> getInvokeArgs() {
        return invokeArgs;
    }

    public void setInvokeArgs(List<String> invokeArgs) {
        this.invokeArgs = invokeArgs;
    }

    public List<String> getRecoverArgs() {
        return recoverArgs;
    }

    public void setRecoverArgs(List<String> recoverArgs) {
        this.recoverArgs = recoverArgs;
    }
}
