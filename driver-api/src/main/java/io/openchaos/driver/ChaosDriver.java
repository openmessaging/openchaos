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

package io.openchaos.driver;
import io.openchaos.MetaDataSupport;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ChaosDriver extends MetaDataSupport {

    /**
     * Driver implementation can use this method to initialize the distributed system libraries, with the provided
     * configuration file.
     *
     * @param configurationFile where to load configuration
     * @param nodes nodes of the distributed system to be tested
     * @throws IOException throws exception when load error
     */
    void initialize(File configurationFile, List<String> nodes) throws IOException;

    /**
     * Shutdown the ChaosDriver
     */
    void shutdown();

    /**
     * Create a ChaosNode. ChaosNode represents one of the nodes in the cluster to be tested
     *
     * @param node current node
     * @param nodes all the nodes of the distributed system to be tested
     */
    ChaosNode createChaosNode(String node, List<String> nodes);

    /**
     * Create a metaNode. MetaNode represents one of the auxiliary nodes of the cluster to be tested. eg:
     * Zookeeper for Kafka, Nameserver for RocketMQ If it does not exist, can return null
     *
     * @param node current node
     * @param nodes all the nodes of MetaNodes
     */
    default MetaNode createChaosMetaNode(String node, List<String> nodes) {
        return null;
    }
    
     /**
     * Get the ClassName of the state class
     */
    String getStateName(); 
}
