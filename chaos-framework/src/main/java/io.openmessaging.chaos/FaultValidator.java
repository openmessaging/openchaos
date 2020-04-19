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

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FaultValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        Set<String> faultSet = new HashSet<>(Arrays.asList(
            "noop", "minor-kill", "major-kill", "random-kill", "fixed-kill", "random-partition", "fixed-partition", "partition-majorities-ring",
            "bridge", "random-loss", "minor-suspend", "major-suspend", "random-suspend", "fixed-suspend"));
        if (!faultSet.contains(value))
            throw new ParameterException("Fault must be one of " + faultSet);
    }
}