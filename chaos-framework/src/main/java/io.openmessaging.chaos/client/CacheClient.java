package io.openmessaging.chaos.client;

import io.openmessaging.chaos.driver.cache.CacheChaosClient;
import io.openmessaging.chaos.driver.cache.CacheChaosDriver;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheClient implements Client {

    private static final AtomicInteger CLIENT_ID_GENERATOR = new AtomicInteger(0);
    private static final Logger log = LoggerFactory.getLogger(CacheClient.class);

    private CacheChaosClient cacheChaosClient;
    private CacheChaosDriver cacheChaosDriver;

    private Optional<String> key;

    public CacheClient(CacheChaosDriver cacheChaosDriver) {
        this.cacheChaosDriver = cacheChaosDriver;
    }

    @Override public void setup() {
        if (cacheChaosDriver == null) {
            throw new IllegalArgumentException("cacheChaosDriver is null when setup CacheClient");
        }
        cacheChaosClient = cacheChaosDriver.createCacheChaosClient();
        cacheChaosClient.start();
    }

    @Override public void teardown() {
        cacheChaosClient.close();
    }

    @Override public void nextInvoke() {

    }

    @Override public void lastInvoke() {

    }
}
