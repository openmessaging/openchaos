package io.openchaos.checker.result;

import java.util.Map;
import java.util.Set;

//public class NacosTestResult extends TestResult{
//    public long putInvokeCount;
//    public long pubSuccessCount;
//    public long subSuccessCount;
//    public long lostValueCount=0;
//    public Set<String> lostValues;
//    public long WrongSequence;
//    public Map<String,Long> Wrong;
//    public long subTimeOutCount;
//    public Set<String> subTimeOutSet;
//    public long missValueCount;
//    public Set<String> missValues;
//
//    public NacosTestResult() {
//        super("NacosTestResult");
//    }
//}
public class NacosTestResult extends TestResult{
    public long putInvokeCount;
    public long pubSuccessCount;
    public long subSuccessCount;
    public long subTimeOutCount;
    public Set<String> subTimeOutValues;
    public long lostValueCount=0;
    public Set<String> lostValues;
    public long unOrderCount;
    public Map<String,Long> unOrderValues;
    public long missValueCount;
    public Set<String> missValues;

    public NacosTestResult() {
        super("NacosTestResult");
    }
}
