package io.openchaos.model;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.util.concurrent.RateLimiter;
import io.openchaos.DriverConfiguration;
import io.openchaos.client.NacosClient;
import io.openchaos.client.Client;
import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.nacos.NacosDriver;
import io.openchaos.recorder.Recorder;
import io.openchaos.worker.ClientWorker;
import io.openchaos.worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.openchaos.driver.nacos.NacosChaosDriver;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Optional;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * @author baozi
 */
public class NacosModel implements Model{
    public static final String MODEL_NAME = "Nacos";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(NacosModel.class);
    private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private final List<NacosClient> clients;
    private List<ClientWorker> workers;
    private Map<String, ChaosNode> cluster;
    private RateLimiter rateLimiter;
    private Map<String, ChaosNode> metaNodesMap;
    private int concurrency;
    private Recorder recorder;
    private NacosDriver nacosDriver;
    private File driverConfigFile;
    private Optional<String> key;
    public NacosModel(int concurrency, RateLimiter rateLimiter, Recorder recorder, File driverConfigFile) {

        this.concurrency = concurrency;
        this.clients = new ArrayList<>();
        this.workers = new ArrayList<>();
        this.cluster = new HashMap<>();
        this.rateLimiter = rateLimiter;
        this.recorder = recorder;
        this.metaNodesMap = new HashMap<>();
        this.driverConfigFile = driverConfigFile;
        this.key = Optional.ofNullable(String.format("%s-chaos-key", DATE_FORMAT.format(new Date())));
    }

    private NacosDriver createNacosDriver(File driverConfigFile) throws IOException {

        DriverConfiguration driverConfiguration = MAPPER.readValue(driverConfigFile, DriverConfiguration.class);
        log.info("Initial driver: {}", WRITER.writeValueAsString(driverConfiguration));

        NacosDriver nacosDriver;

        try {
            nacosDriver = (NacosDriver) Class.forName(driverConfiguration.driverClass).newInstance();
            nacosDriver.initialize(driverConfigFile, driverConfiguration.nodes);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            log.error("Create Nacos Chaos Driver fail", e);
            throw new RuntimeException(e);
        }

        return nacosDriver;
    }

    @Override
    public void setupClient() {
        try {
            if (nacosDriver == null) {
                nacosDriver = createNacosDriver(driverConfigFile);
            }

            log.info("Nacos clients setup..");

//          //Set<String> accounts = nacosChaosDriver.getAccounts();

            for (int i = 0; i < concurrency; i++) {
                NacosClient client = new NacosClient(nacosDriver, recorder,key);
                client.setup();
                clients.add(client);
                ClientWorker clientWorker = new ClientWorker("NacosClient-" + i, client, rateLimiter, log);
                workers.add(clientWorker);
            }

            log.info("{} Nacos clients setup success", concurrency);

        } catch (Exception e) {
            log.error("Nacos model setupClient fail", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, ChaosNode> setupCluster(DriverConfiguration driverConfiguration, boolean isInstall) {
        try {
            if (nacosDriver == null) {
                nacosDriver = createNacosDriver(driverConfigFile);
            }

            if (driverConfiguration.metaNodes != null) {
                driverConfiguration.metaNodes.forEach(node -> metaNodesMap.put(node, nacosDriver.createChaosMetaNode(node, driverConfiguration.metaNodes)));
            }

            if (driverConfiguration.nodes != null) {
                driverConfiguration.nodes.forEach(node -> cluster.put(node, nacosDriver.createChaosNode(node, driverConfiguration.nodes)));
            }

            if (isInstall) {
                metaNodesMap.values().forEach(ChaosNode::setup);
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

            if (driverConfiguration.metaNodesParticipateInFault) {
                Map<String, ChaosNode> allNodes = new HashMap<>(metaNodesMap);
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
        log.info("Nacos chaos test stop");
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
        metaNodesMap.values().forEach(ChaosNode::teardown);
        if (nacosDriver != null) {
            nacosDriver.shutdown();
        }
    }
    @Override
    public String getStateName() {
        return null;
    }

    @Override
    public String getMetaNode() { return null; }

    @Override
    public String getMetaName() {
        return null;
    }

}
