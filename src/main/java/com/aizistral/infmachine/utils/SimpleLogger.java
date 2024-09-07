package com.aizistral.infmachine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLogger {
    private final Logger logger;

    public SimpleLogger(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    public void error(String error) {
        this.logger.error(error);
    }

    public void error(String error, Object... args) {
        this.logger.error(this.insertArgs(error, args));
    }

    public void info(String log) {
        this.logger.info(log);
    }

    public void info(String log, Object... args) {
        this.logger.info(this.insertArgs(log, args));
    }

    public void debug(String log) {
        this.logger.debug(log);
    }

    public void debug(String log, Object... args) {
        this.logger.debug(this.insertArgs(log, args));
    }

    public void error(String error, Throwable ex) {
        this.logger.error(error, ex);
    }

    public void error(String error, Throwable ex, Object... args) {
        this.logger.error(this.insertArgs(error, args), ex);
    }

    private String insertArgs(String str, Object... args) {
        return String.format(str.replace("{}", "%s"), args);
    }

}
