package com.careerplan.elasticsearch.service;

import com.careerplan.elasticsearch.dto.FullTextSearchRequest;
import com.careerplan.elasticsearch.dto.StructuredSearchRequest;
import org.elasticsearch.action.search.SearchResponse;

import java.io.IOException;

/**
 * 商品查询接口
 *
 * @author zhonghuashishan
 */
public interface ProductService {

    /**
     * 全文检索接口
     *
     * @param request com.careerplan.elasticsearch.api.dto.FullTextSearchDto
     * @return org.elasticsearch.action.search.SearchResponse
     */
    SearchResponse fullTextSearch(FullTextSearchRequest request) throws IOException;


    /**
     * 商品结构化搜索接口
     *
     * @param request com.careerplan.elasticsearch.api.dto.StructuredSearchDto
     * @return org.elasticsearch.action.search.SearchResponse
     */
    SearchResponse structuredSearch(StructuredSearchRequest request) throws IOException;
}