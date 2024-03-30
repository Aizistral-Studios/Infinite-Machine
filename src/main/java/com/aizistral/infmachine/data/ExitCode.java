package com.aizistral.infmachine.data;

public enum ExitCode {
    CONFIG_ERROR(2),
    DATABASE_ERROR(3),
    MISSING_DOMAIN_ERROR(13),
    ;

    private final int code;

    ExitCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}