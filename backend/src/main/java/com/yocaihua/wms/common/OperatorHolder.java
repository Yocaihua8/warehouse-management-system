package com.yocaihua.wms.common;

public final class OperatorHolder {

    private static final String DEFAULT_OPERATOR = "admin";

    private OperatorHolder() {
    }

    public static String getCurrentOperator() {
        return CurrentUserContext.getUsernameOrDefault(DEFAULT_OPERATOR);
    }
}
