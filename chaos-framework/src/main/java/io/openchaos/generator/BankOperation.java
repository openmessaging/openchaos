package io.openchaos.generator;

/**
 * @author baozi
 */
public class BankOperation {

    private String invokeOperation;

    private String outAccount;

    private String inAccount;

    private int amt;

    public BankOperation(String invokeOperation, String outAccount, String inAccount, int amt) {
        this.invokeOperation = invokeOperation;
        this.outAccount = outAccount;
        this.inAccount = inAccount;
        this.amt = amt;
    }

    public String getInvokeOperation() {
        return invokeOperation;
    }

    public void setInvokeOperation(String invokeOperation) {
        this.invokeOperation = invokeOperation;
    }

    public String getOutAccount() {
        return outAccount;
    }

    public void setOutAccount(String outAccount) {
        this.outAccount = outAccount;
    }

    public String getInAccount() {
        return inAccount;
    }

    public void setInAccount(String inAccount) {
        this.inAccount = inAccount;
    }

    public int getAmt() {
        return amt;
    }

    public void setAmt(int amt) {
        this.amt = amt;
    }

    public String getValue() {
        return outAccount + "-to-" + inAccount + "-" + amt;
    }

    @Override
    public String toString() {
        return "BankOperation{" +
                "invokeOperation='" + invokeOperation + '\'' +
                ", outAccount='" + outAccount + '\'' +
                ", inAccount='" + inAccount + '\'' +
                ", amt=" + amt +
                '}';
    }
}
