package io.openmessaging.driver.rabbitmq.utils;

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

    public ChannelPoolFactory(ConnectionFactory factory) {
        this.factory = factory;
        connection = createNewConnection();
    }

    @Override
    public Object create() {
        try {
            return connection.createChannel();
        } catch (IOException e) {
            log.warn("IO blocked!");
        } catch (Exception e) {
            log.warn("Unknow exception occured");
            connection = createNewConnection();
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
        object.close();
    }

    public Connection createNewConnection() {
        try {
            connection = factory.newConnection();
        } catch (IOException e) {
            log.warn("IO blocked");
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
