package io.openchaos.driver.nacos;

import com.alibaba.nacos.api.config.listener.Listener;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author yixi.cll
 * @date 2021/5/28
 */
public class DefaultConfigListener implements Listener {
    protected static final Logger         log                    = LoggerFactory
            .getLogger(DefaultConfigListener.class);
    static                 int            DEFAULT_AWAIT_TIME_OUT = 30;
    private                String         name                   = "";
    private                String         receiveContent;

    public DefaultConfigListener(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }


    public String getReceiveContent() {
        return receiveContent;
    }

    @Override
    public Executor getExecutor() {
        return null;
    }

    @Override
    public void receiveConfigInfo(String s) {

        receiveContent = s;

        log.info(name +" receive content: " + receiveContent);
        System.out.println(name +" receive content: " + receiveContent);


    }

}
