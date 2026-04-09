package com.yocaihua.wms.desktop.module.supplier;

import java.util.ArrayList;
import java.util.List;

public class SupplierPageData {

    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<SupplierRow> list = new ArrayList<>();

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

    public List<SupplierRow> getList() {
        return list;
    }

    public void setList(List<SupplierRow> list) {
        this.list = list == null ? new ArrayList<>() : list;
    }
}
