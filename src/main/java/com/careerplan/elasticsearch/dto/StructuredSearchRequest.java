package com.careerplan.elasticsearch.dto;

import lombok.Data;

import java.util.Map;

/**
 * @author zhonghuashishan
 */
@Data
public class StructuredSearchRequest {

    /**
     * 索引名字
     */
    private String indexName;

    /**
     * Query DSL：ES查询语法，是按照JSON来组织
     * 按照elasticsearch的规范写的query dsl，是一个json对象，
     * 解析的时候转成json字符串，客户端api可以直接解析字符串
     */
    private Map<String, Object> queryDsl;

    /**
     * 当前页
     */
    private int pageNum;

    /**
     * 每页条数
     */
    private int pageSize;
}
