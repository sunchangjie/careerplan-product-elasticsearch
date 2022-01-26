package com.careerplan.elasticsearch.common.enums;

/**
 * 商品状态枚举
 *
 * @author zhonghuashishan
 */
public enum ItemStatusEnum {

    /**
     * 准备上架
     */
    PRE_ONLINE("1","准备上架"),

    /**
     * 试销上架
     */
    TRY_ONLINE("2","试销上架"),

    /**
     * 上架
     */
    ONLINE("3","上架"),

    /**
     * 预下架
     */
    PRE_OFFLINE("4","预下架"),

    /**
     * 下架
     */
    OFFLINE("5","下架"),

    /**
     * 停售
     */
    HALT_SALES("6","停售"),
    ;

    private String code;
    private String message;

    ItemStatusEnum(String code, String message){
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
