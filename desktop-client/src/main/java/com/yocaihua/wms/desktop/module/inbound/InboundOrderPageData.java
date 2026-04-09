package com.yocaihua.wms.desktop.module.inbound;

import java.util.ArrayList;
import java.util.List;

public class InboundOrderPageData {

    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<InboundOrderRow> list = new ArrayList<>();

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<InboundOrderRow> getList() {
        return list;
    }

    public void setList(List<InboundOrderRow> list) {
        this.list = list == null ? new ArrayList<>() : list;
    }
}
