package io.openchaos.driver.bank;

import io.openchaos.common.InvokeResult;
import io.openchaos.driver.ChaosClient;

import java.util.List;

/**
 * @author baozi
 */
public interface BankChaosClient extends ChaosClient {

    /**
     * transfer account amt
     * @param outAccount
     * @param inAccount
     * @param amt
     * @return
     */
    InvokeResult transfer(String outAccount, String inAccount, int amt);

    /**
     * get all account amt
     * @param accounts
     * @return
     */
    List<Integer> getAllAmt(List<String> accounts);
}
