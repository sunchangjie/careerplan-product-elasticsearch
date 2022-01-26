package com.careerplan.elasticsearch.common.enums;

import lombok.Getter;

/**
 * 查询类型枚举
 *
 * @author zhonghuashishan
 */
@Getter
public enum QueryTypeEnum {

    /**
     * 查询采用模糊查询
     */
    FUZZY(1,"模糊查询"),

    /**
     * 查询不采用模糊查询
     */
    NOT_FUZZY(2,"不模糊查询")
    ;

    Integer code;
    String message;

    QueryTypeEnum(Integer code, String message){
        this.code = code;
        this.message = message;
    }

}
