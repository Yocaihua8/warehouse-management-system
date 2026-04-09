package com.yocaihua.wms.common;

public class OrderStatusConstant {

    private OrderStatusConstant() {
    }

    public static final Integer INBOUND_DRAFT = 1;
    public static final Integer INBOUND_COMPLETED = 2;
    public static final Integer INBOUND_VOID = 3;

    public static final Integer OUTBOUND_DRAFT = 1;
    public static final Integer OUTBOUND_COMPLETED = 2;
    public static final Integer OUTBOUND_VOID = 3;
}
