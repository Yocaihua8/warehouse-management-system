package com.yocaihua.wms.desktop.module.product;

import java.util.ArrayList;
import java.util.List;

public class ProductPageData {

    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<ProductRow> list = new ArrayList<>();

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

    public List<ProductRow> getList() {
        return list;
    }

    public void setList(List<ProductRow> list) {
        this.list = list == null ? new ArrayList<>() : list;
    }
}
