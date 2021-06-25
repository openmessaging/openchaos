package io.openchaos.client;

import io.openchaos.common.InvokeResult;
import io.openchaos.driver.bank.BankChaosClient;
import io.openchaos.driver.bank.BankChaosDriver;
import io.openchaos.generator.BankOperation;
import io.openchaos.generator.SequenceGenerator;
import io.openchaos.recorder.Recorder;
import io.openchaos.recorder.RequestLogEntry;
import io.openchaos.recorder.ResponseLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author baozi
 */
public class BankClient implements Client{

    private static final AtomicInteger CLIENT_ID_GENERATOR = new AtomicInteger(0);
    private static final Logger log = LoggerFactory.getLogger(BankClient.class);

    private BankChaosClient bankChaosClient;
    private BankChaosDriver bankChaosDriver;
    private Recorder recorder;
    private int clientId;

    private List<String> accounts;

    public BankClient(BankChaosDriver bankChaosDriver, Recorder recorder, Set<String> accounts) {
        this.bankChaosDriver = bankChaosDriver;
        this.recorder = recorder;
        this.accounts = new ArrayList<>(accounts);
        this.clientId = CLIENT_ID_GENERATOR.getAndIncrement();
    }

    @Override
    public void setup() {
        if (bankChaosDriver == null) {
            throw new IllegalArgumentException("bankChaosDriver is null when setup BankClient");
        }
        bankChaosClient = bankChaosDriver.createBankChaosClient();
        bankChaosClient.start();
    }

    @Override
    public void teardown() {
        bankChaosClient.close();
    }

    @Override
    public void nextInvoke() {
        BankOperation bankOperation = SequenceGenerator.generateBankOperation(accounts);
        RequestLogEntry requestLogEntry = new RequestLogEntry(clientId, bankOperation.getInvokeOperation(), bankOperation.getValue(), System.currentTimeMillis());
        recorder.recordRequest(requestLogEntry);
        InvokeResult result = bankChaosClient.transfer(bankOperation.getOutAccount(), bankOperation.getInAccount(), bankOperation.getAmt());
        recorder.recordResponse(new ResponseLogEntry(clientId, bankOperation.getInvokeOperation(), result, bankOperation.getValue(), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
    }

    @Override
    public void lastInvoke() {
        RequestLogEntry requestLogEntry = new RequestLogEntry(clientId, "getAllAmt", accounts.toString(), System.currentTimeMillis());
        recorder.recordRequest(requestLogEntry);
        List<Integer> results = bankChaosClient.getAllAmt(accounts);
        if (results != null && !results.isEmpty()) {
            recorder.recordResponse(new ResponseLogEntry(clientId, "getAllAmt", InvokeResult.SUCCESS, results.toString(), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
        } else {
            recorder.recordResponse(new ResponseLogEntry(clientId, "getAllAmt", InvokeResult.FAILURE, null, System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
        }
    }
}
