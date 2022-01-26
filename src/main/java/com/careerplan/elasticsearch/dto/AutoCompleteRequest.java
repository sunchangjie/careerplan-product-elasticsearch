package com.careerplan.elasticsearch.dto;

import lombok.Data;


/**
 * @author zhonghuashishan
 */
@Data
public class AutoCompleteRequest {

    /**
     * 索引名称
     */
    private String indexName;

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 需要补全的词(用户输入的内容)
     */
    private String text;

    /**
     * 返回多少个补全后的词
     */
    private int count;
}
