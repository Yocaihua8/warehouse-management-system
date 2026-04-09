package com.yocaihua.wms.service;

public interface LowStockAlertService {

    String triggerNow();

    String checkAndNotify(boolean force);
}
