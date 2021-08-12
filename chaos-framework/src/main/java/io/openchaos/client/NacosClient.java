package io.openchaos.client;

import io.openchaos.common.InvokeResult;
import io.openchaos.common.NacosMessage;
import io.openchaos.driver.nacos.NacosCallback;
import io.openchaos.driver.nacos.NacosDriver;
import io.openchaos.generator.MD5;
import io.openchaos.generator.NacosOperation;
import io.openchaos.generator.Operation;
import io.openchaos.generator.SequenceGenerator;
import io.openchaos.recorder.Recorder;
import io.openchaos.recorder.RequestLogEntry;
import io.openchaos.recorder.ResponseLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class NacosClient implements Client, NacosCallback {
    private static final AtomicInteger CLIENT_ID_GENERATOR = new AtomicInteger(0);
    private static final AtomicInteger PUT_COUNT = new AtomicInteger(0);
    private static final Logger log = LoggerFactory.getLogger(KVClient.class);

    private io.openchaos.driver.nacos.NacosClient client;
    private final NacosDriver driver;
    private final Recorder recorder;
    private final int clientId;
    private final Optional<String> key;
    private String dataId;
    private String group;
    private int NUM;
    private int threshold ;
    public NacosClient(NacosDriver driver, Recorder recorder, Optional<String> key) {
        this.driver = driver;
        this.recorder = recorder;
        this.clientId = CLIENT_ID_GENERATOR.getAndIncrement();
        this.key = key;
    }
    public void setup() {
        if (driver == null) {
            throw new IllegalArgumentException("Nacos driver is null");
        }
        client = driver.createClient(clientId,this);
        log.info("Nacos client start...");
        client.start();
        dataId = driver.getdataIds().get(clientId); //String
        group = driver.getgroup().get(0);//String
        NUM = driver.getNUM();
    }

    public void teardown() {
        log.info("Nacos client teardown...");
        client.close();
    }

    public void nextInvoke() {
        List<String> list = new ArrayList<String>();
        list.add(dataId);
        list.add(group);
        list.add(Integer.toString(NUM));

        System.out.println("Config canshu:"+list.get(0)+list.get(1)+list.get(2));
        NacosOperation op = SequenceGenerator.generateNacosOperation(list);
        InvokeResult result = client.put(key, op.getDataID(),op.getGroup(),op.getPubConfig());
        RequestLogEntry requestLogEntry = new RequestLogEntry(clientId, result,op.getInvokeOperation(), op.getValue(),client.getSendtimestamp());
        recorder.recordRequestNacos(requestLogEntry);
        PUT_COUNT.getAndIncrement();
        //recorder.recordResponse(new ResponseLogEntry(clientId, op.getInvokeOperation(), result, op.getValue(), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
    }

    public void lastInvoke() {
//        RequestLogEntry requestLogEntry = new RequestLogEntry(clientId, "getAll", null, System.currentTimeMillis());
//        recorder.recordRequest(requestLogEntry);
//        List<String> results = client.getAll(key, PUT_COUNT.get());
//        if (results != null && !results.isEmpty()) {
//            recorder.recordResponse(new ResponseLogEntry(clientId, "getAll", InvokeResult.SUCCESS, results.toString(), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
//        } else {
//            recorder.recordResponse(new ResponseLogEntry(clientId, "getAll", InvokeResult.FAILURE, null, System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
//        }
    }

    @Override
    public void messageReceived(NacosMessage message) {
        String value = "dataID\t"+message.dataId + "\tgroup\t" + message.group + "\tpubConfig\t" + MD5.MD5_16(message.config);

        recorder.recordResponseNacos(new ResponseLogEntry(clientId, "receive", message.result, value, System.currentTimeMillis(),
                message.subTimestamp-message.pubTimestamp,message.pubTimestamp));
    }
}
