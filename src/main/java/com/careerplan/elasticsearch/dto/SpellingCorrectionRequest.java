package com.careerplan.elasticsearch.dto;

import lombok.Data;


/**
 * @author zhonghuashishan
 */
@Data
public class SpellingCorrectionRequest {

    /**
     * 索引名称
     */
    private String indexName;

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 用户输入的内容
     */
    private String text;
}
