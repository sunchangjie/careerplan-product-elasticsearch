package com.careerplan.elasticsearch.dto;

import lombok.Data;


/**
 * @author zhonghuashishan
 */
@Data
public class MockData1Dto {

    /**
     * 索引名称
     */
    private String indexName;

    /**
     * 一次批量插入的文档数
     */
    private int batchSize;

    /**
     * 执行批量插入的次数
     */
    private int batchTimes;

    public boolean validateParams() {
        if (indexName == null || indexName.trim().length() == 0) {
            return false;
        }
        if (batchSize <= 0 ||  batchTimes <= 0) {
            return false;
        }
        return true;
    }
}
