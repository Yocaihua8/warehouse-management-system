package com.yocaihua.wms;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class WarehouseManagementSystemApplicationTests {

    @Test
    void mainMethodShouldExist() {
        assertDoesNotThrow(() -> {
            Method mainMethod = WarehouseManagementSystemApplication.class.getDeclaredMethod("main", String[].class);
            mainMethod.setAccessible(true);
        });
    }

}
