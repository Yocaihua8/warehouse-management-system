package com.yocaihua.wms.common;

public final class UserRoleConstant {

    public static final String ADMIN = "ADMIN";
    public static final String OPERATOR = "OPERATOR";

    private UserRoleConstant() {
    }

    public static String normalize(String role) {
        if (role == null || role.trim().isEmpty()) {
            return OPERATOR;
        }
        String normalized = role.trim().toUpperCase();
        if (ADMIN.equals(normalized)) {
            return ADMIN;
        }
        return OPERATOR;
    }
}
