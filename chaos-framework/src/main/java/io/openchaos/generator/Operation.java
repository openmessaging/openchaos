/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openchaos.generator;

public class Operation {

    private String invokeOperation;
    private String value;

    public Operation(String invokeOperation) {
        this.invokeOperation = invokeOperation;
    }

    public Operation(String invokeOperation, String value) {
        this.invokeOperation = invokeOperation;
        this.value = value;
    }

    public String getInvokeOperation() {
        return invokeOperation;
    }

    public void setInvokeOperation(String invokeOperation) {
        this.invokeOperation = invokeOperation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override public String toString() {
        return "Operation{" +
            "invokeOperation='" + invokeOperation + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
