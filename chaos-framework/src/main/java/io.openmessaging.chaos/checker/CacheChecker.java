package io.openmessaging.chaos.checker;

import io.openmessaging.chaos.checker.result.TestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheChecker implements Checker{

    private static final Logger log = LoggerFactory.getLogger(CacheChecker.class);
    private String outputDir;
    private String fileName;

    public CacheChecker(String outputDir, String fileName) {
        this.outputDir = outputDir;
        this.fileName = fileName;
    }

    @Override public TestResult check() {
        return null;
    }
}
