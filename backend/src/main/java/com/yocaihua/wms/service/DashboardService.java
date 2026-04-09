package com.yocaihua.wms.service;

import com.yocaihua.wms.vo.DashboardVO;
import com.yocaihua.wms.vo.DashboardTrendPointVO;

import java.util.List;

public interface DashboardService {

    DashboardVO getDashboardData();

    List<DashboardTrendPointVO> getRecentTrend(int days);
}
