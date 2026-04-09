package com.yocaihua.wms.desktop.ui.layout;

public enum ShellModule {

    HOME("工作台", "工作台", "这里放桌面端首页概览、常用入口和系统提示。", "下一步适合先接首页统计和最近单据入口。"),
    PRODUCT("商品管理", "商品管理", "这里将承载商品查询、商品编辑和基础资料维护。", "下一步适合先接商品列表查询和商品新增弹窗。"),
    CUSTOMER("客户管理", "客户管理", "这里将承载客户查询、客户维护和出库相关基础资料管理。", "下一步适合先接客户列表和客户新增编辑弹窗。"),
    SUPPLIER("供应商管理", "供应商管理", "这里将承载供应商查询、供应商新增和入库相关基础资料管理。", "下一步适合先接供应商列表和供应商新增弹窗。"),
    STOCK("库存管理", "库存管理", "这里将承载库存查询、库存流水和低库存预警。", "下一步适合优先接库存列表页。"),
    INBOUND("入库管理", "入库管理", "这里将承载手工入库、AI 入库确认和入库单详情。", "下一步适合先接入库单列表和入库草稿页。"),
    OUTBOUND("出库管理", "出库管理", "这里将承载手工出库、AI 出库确认和出库单详情。", "下一步适合先接出库单列表和出库草稿页。"),
    AI("AI识别", "AI识别", "这里将承载 AI 识别历史、继续确认和业务助手入口。", "下一步适合先接 AI 入库历史和确认页。"),
    SETTINGS("系统设置", "系统设置", "这里用于维护服务地址、连接测试和本地配置。", "下一步适合先接本地服务编排和数据库初始化引导。");

    private final String navTitle;
    private final String pageTitle;
    private final String description;
    private final String nextStepHint;

    ShellModule(String navTitle, String pageTitle, String description, String nextStepHint) {
        this.navTitle = navTitle;
        this.pageTitle = pageTitle;
        this.description = description;
        this.nextStepHint = nextStepHint;
    }

    public String getNavTitle() {
        return navTitle;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public String getDescription() {
        return description;
    }

    public String getNextStepHint() {
        return nextStepHint;
    }
}
