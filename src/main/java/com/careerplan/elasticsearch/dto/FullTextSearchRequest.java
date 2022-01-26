package com.careerplan.elasticsearch.dto;

import lombok.Data;

import java.util.Map;

/**
 * @author zhonghuashishan
 */
@Data
public class FullTextSearchRequest {

    /**
     * 索引名字
     */
    private String indexName;

    /**
     * 查询参数
     * key为字段的名字，value为字段的关键词
     * 可以指定从哪些字段里检索
     */
    private Map<String, String> queryTexts;

    /**
     * 高亮字段
     */
    private String highLightField;

    /**
     * 当前页
     */
    private int pageNum;

    /**
     * 每页条数
     */
    private int pageSize;
}
