package com.yocaihua.wms.common;

public final class StockChangeTypeConstant {

    private StockChangeTypeConstant() {
    }

    public static final String MANUAL_ADJUST = "MANUAL_ADJUST";

    public static final String MANUAL_INBOUND = "MANUAL_INBOUND";

    public static final String AI_CONFIRM_INBOUND = "AI_CONFIRM_INBOUND";

    public static final String MANUAL_OUTBOUND = "MANUAL_OUTBOUND";

    public static final String AI_CONFIRM_OUTBOUND = "AI_CONFIRM_OUTBOUND";

    public static final String VOID_INBOUND = "VOID_INBOUND";

    public static final String VOID_OUTBOUND = "VOID_OUTBOUND";
}
