package com.yocaihua.wms.common;

public final class CurrentUserContext {

    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            CURRENT_USERNAME.remove();
            return;
        }
        CURRENT_USERNAME.set(username.trim());
    }

    public static void setRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            CURRENT_ROLE.remove();
            return;
        }
        CURRENT_ROLE.set(UserRoleConstant.normalize(role));
    }

    public static String getUsername() {
        return CURRENT_USERNAME.get();
    }

    public static String getRole() {
        return CURRENT_ROLE.get();
    }

    public static boolean isAdmin() {
        return UserRoleConstant.ADMIN.equals(getRole());
    }

    public static String getUsernameOrDefault(String defaultUsername) {
        String username = CURRENT_USERNAME.get();
        if (username == null || username.trim().isEmpty()) {
            return defaultUsername;
        }
        return username;
    }

    public static void clear() {
        CURRENT_USERNAME.remove();
        CURRENT_ROLE.remove();
    }
}
