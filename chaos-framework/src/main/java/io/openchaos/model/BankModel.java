package io.openchaos.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.util.concurrent.RateLimiter;
import io.openchaos.DriverConfiguration;
import io.openchaos.client.BankClient;
import io.openchaos.client.CacheClient;
import io.openchaos.client.Client;
import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.bank.BankChaosDriver;
import io.openchaos.recorder.Recorder;
import io.openchaos.worker.ClientWorker;
import io.openchaos.worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author baozi
 */
public class BankModel implements Model{

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(CacheModel.class);
    private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private final List<Client> clients;
    private List<ClientWorker> workers;
    private Map<String, ChaosNode> cluster;
    private RateLimiter rateLimiter;
    private Map<String, ChaosNode> preNodesMap;
    private int concurrency;
    private Recorder recorder;
    private BankChaosDriver bankChaosDriver;
    private File driverConfigFile;

    public BankModel(int concurrency, RateLimiter rateLimiter, Recorder recorder, File driverConfigFile) {
        this.concurrency = concurrency;
        this.clients = new ArrayList<>();
        this.workers = new ArrayList<>();
        this.cluster = new HashMap<>();
        this.rateLimiter = rateLimiter;
        this.recorder = recorder;
        this.preNodesMap = new HashMap<>();
        this.driverConfigFile = driverConfigFile;
    }

    private BankChaosDriver createBankChaosDriver(File driverConfigFile) throws IOException {

        DriverConfiguration driverConfiguration = MAPPER.readValue(driverConfigFile, DriverConfiguration.class);
        log.info("Initial driver: {}", WRITER.writeValueAsString(driverConfiguration));

        BankChaosDriver bankChaosDriver;

        try {
            bankChaosDriver = (BankChaosDriver) Class.forName(driverConfiguration.driverClass).newInstance();
            bankChaosDriver.initialize(driverConfigFile, driverConfiguration.nodes);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            log.error("Create Cache Chaos Driver fail", e);
            throw new RuntimeException(e);
        }

        return bankChaosDriver;
    }

    @Override
    public void setupClient() {
        try {
            if (bankChaosDriver == null) {
                bankChaosDriver = createBankChaosDriver(driverConfigFile);
            }

            log.info("Cache clients setup..");

            Set<String> accounts = bankChaosDriver.getAccounts();

            for (int i = 0; i < concurrency; i++) {
                Client client = new BankClient(bankChaosDriver, recorder, accounts);
                client.setup();
                clients.add(client);
                ClientWorker clientWorker = new ClientWorker("cacheClient-" + i, client, rateLimiter, log);
                workers.add(clientWorker);
            }

            log.info("{} cache clients setup success", concurrency);

        } catch (Exception e) {
            log.error("Cache model setupClient fail", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, ChaosNode> setupCluster(DriverConfiguration driverConfiguration, boolean isInstall) {
        try {
            if (bankChaosDriver == null) {
                bankChaosDriver = createBankChaosDriver(driverConfigFile);
            }

            if (driverConfiguration.preNodes != null) {
                driverConfiguration.preNodes.forEach(node -> preNodesMap.put(node, bankChaosDriver.createPreChaosNode(node, driverConfiguration.preNodes)));
            }

            if (driverConfiguration.nodes != null) {
                driverConfiguration.nodes.forEach(node -> cluster.put(node, bankChaosDriver.createChaosNode(node, driverConfiguration.nodes)));
            }

            if (isInstall) {
                preNodesMap.values().forEach(ChaosNode::setup);
                cluster.values().forEach(ChaosNode::setup);
            }

            log.info("Cluster shutdown");
            cluster.values().forEach(ChaosNode::teardown);
//            preNodesMap.values().forEach(ChaosNode::stop);
//            log.info("Wait for all nodes to shutdown...");
//            try {
//                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
//            } catch (InterruptedException e) {
//                log.error("", e);
//            }

            log.info("Cluster start...");

//            log.info("Wait for all preNodes to start...");
//            preNodesMap.values().forEach(ChaosNode::start);
//            try {
//                Thread.sleep(TimeUnit.SECONDS.toMillis(20));
//            } catch (InterruptedException e) {
//                log.error("", e);
//            }
            log.info("Wait for all nodes to start...");
            cluster.values().forEach(ChaosNode::start);
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(60));
            } catch (InterruptedException e) {
                log.error("", e);
            }

            if (driverConfiguration.preNodesParticipateInFault) {
                Map<String, ChaosNode> allNodes = new HashMap<>(preNodesMap);
                allNodes.putAll(cluster);
                return allNodes;
            } else {
                return cluster;
            }

        } catch (Exception e) {
            log.error("Cache model setupCluster fail", e);
            throw new RuntimeException(e);

        }
    }

    @Override
    public boolean probeCluster() {
        return true;
    }

    @Override
    public void start() {
        log.info("Start all clients...");
        workers.forEach(Thread::start);
    }

    @Override
    public void stop() {
        log.info("Cache chaos test stop");
        workers.forEach(Worker::breakLoop);
    }

    @Override
    public void afterStop() {
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("clients is empty");
        } else {
            clients.get(0).lastInvoke();
        }
    }

    @Override
    public void shutdown() {
       log.info("Teardown client");
       clients.forEach(Client::teardown);
       log.info("Stop cluster");
       cluster.values().forEach(ChaosNode::teardown);
       preNodesMap.values().forEach(ChaosNode::teardown);
       if (bankChaosDriver != null) {
           bankChaosDriver.shutdown();
       }
    }
}
