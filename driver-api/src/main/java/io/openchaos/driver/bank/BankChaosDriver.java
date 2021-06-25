package io.openchaos.driver.bank;

import io.openchaos.driver.ChaosDriver;

import java.util.Set;

/**
 * @author baozi
 */
public interface BankChaosDriver extends ChaosDriver {

    /**
     * create chaos client
     * @return
     */
    BankChaosClient createBankChaosClient();

    /**
     * get all accounts
     * @return
     */
    Set<String> getAccounts();
}
