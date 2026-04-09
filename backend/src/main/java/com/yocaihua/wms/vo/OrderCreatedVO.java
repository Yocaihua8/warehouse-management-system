package com.yocaihua.wms.vo;

public class OrderCreatedVO {

    private Long id;
    private String orderNo;

    public OrderCreatedVO() {
    }

    public OrderCreatedVO(Long id, String orderNo) {
        this.id = id;
        this.orderNo = orderNo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
}
