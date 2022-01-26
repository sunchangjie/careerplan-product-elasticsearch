package com.careerplan.elasticsearch.service;

import com.careerplan.elasticsearch.dto.AutoCompleteRequest;
import com.careerplan.elasticsearch.dto.RecommendWhenMissingRequest;
import com.careerplan.elasticsearch.dto.SpellingCorrectionRequest;

import java.io.IOException;
import java.util.List;

/**
 * 通用查询接口
 *
 * @author zhonghuashishan
 */
public interface CommonSearchService {

    /**
     * 输入内容自动补全接口
     */
    List<String> autoComplete(AutoCompleteRequest request) throws IOException;

    /**
     * 输入内容拼写纠错接口
     * 现在只可以纠正英文
     */
    String spellingCorrection(SpellingCorrectionRequest request) throws IOException;
}
