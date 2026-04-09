package com.yocaihua.wms.service;

import com.yocaihua.wms.vo.SystemBootstrapVO;
import com.yocaihua.wms.vo.SystemHealthVO;

public interface SystemService {

    SystemHealthVO getSystemHealth();

    SystemBootstrapVO getSystemBootstrap();
}
