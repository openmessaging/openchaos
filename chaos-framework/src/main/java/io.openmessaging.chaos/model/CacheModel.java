package io.openmessaging.chaos.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.util.concurrent.RateLimiter;
import io.openmessaging.chaos.DriverConfiguration;
import io.openmessaging.chaos.client.Client;
import io.openmessaging.chaos.driver.cache.CacheChaosDriver;
import io.openmessaging.chaos.driver.mq.MQChaosDriver;
import io.openmessaging.chaos.driver.mq.MQChaosNode;
import io.openmessaging.chaos.recorder.Recorder;
import io.openmessaging.chaos.worker.ClientWorker;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheModel implements Model {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(CacheModel.class);
    private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();
    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private List<Client> clients;
    private List<ClientWorker> workers;
    private Map<String, MQChaosNode> cluster;
    private RateLimiter rateLimiter;
    private int concurrency;
    private Recorder recorder;
    private CacheChaosDriver cacheChaosDriver;
    private File driverConfigFile;


    public CacheModel(int concurrency, RateLimiter rateLimiter, Recorder recorder, File driverConfigFile){
        this.concurrency = concurrency;
        this.clients = new ArrayList<>();
        this.workers = new ArrayList<>();
        this.cluster = new HashMap<>();
        this.rateLimiter = rateLimiter;
        this.recorder = recorder;
        this.driverConfigFile = driverConfigFile;
    }

    private static CacheChaosDriver createCacheChaosDriver(File driverConfigFile) throws IOException {

        DriverConfiguration driverConfiguration = MAPPER.readValue(driverConfigFile, DriverConfiguration.class);
        log.info("Initial driver: {}", WRITER.writeValueAsString(driverConfiguration));

        CacheChaosDriver cacheChaosDriver;

        try {
            cacheChaosDriver = (CacheChaosDriver) Class.forName(driverConfiguration.driverClass).newInstance();
            cacheChaosDriver.initialize(driverConfigFile, driverConfiguration.nodes);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return cacheChaosDriver;
    }

    @Override public void setupClient() {

    }

    @Override public Map<String, MQChaosNode> setupCluster(List<String> nodes, boolean isInstall) {
        try {
            if (cacheChaosDriver == null) {
                cacheChaosDriver = createCacheChaosDriver(driverConfigFile);
            }

            if (nodes != null) {
                nodes.forEach(node -> cluster.put(node, cacheChaosDriver.createChaosNode(node, nodes)));
            }

            if (isInstall) {
                cluster.values().forEach(MQChaosNode::setup);
            }

            log.info("Cluster shutdown");
            cluster.values().forEach(MQChaosNode::stop);
            log.info("Wait for all nodes to shutdown...");
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                log.error("", e);
            }

            log.info("Cluster start...");
            cluster.values().forEach(MQChaosNode::start);
            log.info("Wait for all nodes to start...");
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(20));
            } catch (InterruptedException e) {
                log.error("", e);
            }

            return cluster;
        } catch (Exception e) {
            log.error("Queue model setupCluster fail", e);
            throw new RuntimeException(e);
        }
    }

    @Override public boolean probeCluster() {
        return true;
    }

    @Override public void start() {

    }

    @Override public void stop() {

    }

    @Override public void afterStop() {

    }

    @Override public void shutdown() {

    }
}
