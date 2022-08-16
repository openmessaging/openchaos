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
package io.openchaos.driver.rabbitmq.utils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class ChannelPoolFactory extends BasePooledObjectFactory {
    private static final Logger log = LoggerFactory.getLogger(ChannelPoolFactory.class);
    private ConnectionFactory factory;
    private Connection connection;

    public ChannelPoolFactory(ConnectionFactory factory, Connection connection) {
        this.factory = factory;
        this.connection = connection;
    }

    @Override
    public Object create() {
        try {
            return connection.createChannel();
        } catch (IOException e) {
            log.warn("IO blocked in create channel!");
        } catch (Exception e) {
            log.warn("Unknow exception occured in create channel");
            createNewConnection();
        }
        return null;
    }

    @Override
    public PooledObject wrap(Object o) {
        return new DefaultPooledObject(o);
    }

    @Override
    public void destroyObject(PooledObject p, DestroyMode destroyMode) throws Exception {
        super.destroyObject(p, destroyMode);
        Channel object = (Channel) p.getObject();
        if (object.isOpen()) {
            object.close();
        }
    }

    public Connection createNewConnection() {
        try {
            connection = factory.newConnection("openchaos_channelPool");
        } catch (IOException e) {
            log.warn("IO blocked in create connection");
        } catch (TimeoutException e) {
            log.warn("Create connection timeout");
        }
        return connection;
    }

    @Override
    public boolean validateObject(PooledObject p) {
        return p.getObject() != null && ((Channel) p.getObject()).isOpen();
    }
}
