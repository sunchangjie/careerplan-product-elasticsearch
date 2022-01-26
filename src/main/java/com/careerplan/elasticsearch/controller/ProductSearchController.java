package com.careerplan.elasticsearch.controller;

import com.careerplan.elasticsearch.dto.FullTextSearchRequest;
import com.careerplan.elasticsearch.dto.StructuredSearchRequest;
import com.careerplan.elasticsearch.service.ProductService;
import com.careerplan.elasticsearch.common.core.JsonResult;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品搜索控制类
 *
 * @author zhonghuashishan
 */
@RestController
@RequestMapping("/api/product")
public class ProductSearchController {

    /**
     * 商品服务组件
     */
    @Autowired
    private ProductService productService;

    /**
     * 商品全文检索接口
     */
    @GetMapping("/fullTextSearch")
    public JsonResult fullTextSearch(@RequestBody FullTextSearchRequest request) throws IOException {

        SearchResponse searchResponse = productService.fullTextSearch(request);

        Map<String, Object> resultMap = new HashMap<>();
        SearchHit[] hits = searchResponse.getHits().getHits();
        long totalCount = searchResponse.getHits().getTotalHits().value;
        resultMap.put("hits", hits);
        resultMap.put("totalCount", totalCount);
        resultMap.put("pageNum", request.getPageNum());
        resultMap.put("pageSize", request.getPageSize());

        return JsonResult.buildSuccess(resultMap);
    }

    /**
     * 商品结构化搜索接口
     */
    @GetMapping("/structuredSearch")
    public JsonResult structuredSearch(@RequestBody StructuredSearchRequest request) throws IOException {

        SearchResponse searchResponse = productService.structuredSearch(request);

        Map<String, Object> resultMap = new HashMap<>();
        SearchHit[] hits = searchResponse.getHits().getHits();
        long totalCount = searchResponse.getHits().getTotalHits().value;
        resultMap.put("hits", hits);
        resultMap.put("totalCount", totalCount);
        resultMap.put("pageNum", request.getPageNum());
        resultMap.put("pageSize", request.getPageSize());

        return JsonResult.buildSuccess(resultMap);
    }

}
