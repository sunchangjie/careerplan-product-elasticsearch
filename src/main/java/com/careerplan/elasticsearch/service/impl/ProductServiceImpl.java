package com.careerplan.elasticsearch.service.impl;

import com.alibaba.fastjson.JSON;
import com.careerplan.elasticsearch.dto.FullTextSearchRequest;
import com.careerplan.elasticsearch.dto.StructuredSearchRequest;
import com.careerplan.elasticsearch.service.ProductService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * 商品查询服务实现类
 *
 * @author zhonghuashishan
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public SearchResponse fullTextSearch(FullTextSearchRequest request) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.trackTotalHits(true);

        // 1、构建match条件
        request.getQueryTexts().forEach((field, text) -> {
            searchSourceBuilder.query(QueryBuilders.matchQuery(field, text));
        });

        // 2、设置搜索高亮配置
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(request.getHighLightField());
        highlightBuilder.preTags("<span stype=color:red>"); // 搜索结果里，商品标题（跟你的搜索词匹配的部分会显示为红色）
        highlightBuilder.postTags("</span>");
        highlightBuilder.numOfFragments(0);
        searchSourceBuilder.highlighter(highlightBuilder);

        // 3、设置搜索分页参数
        int from = (request.getPageNum() - 1) * request.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(request.getPageSize());

        // 4、封装搜索请求
        SearchRequest searchRequest = new SearchRequest(request.getIndexName());
        searchRequest.source(searchSourceBuilder);

        // 5、查询elasticsearch
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        // 6、对结果进行高亮处理
        SearchHits hits = searchResponse.getHits();
        for (SearchHit hit : hits) {
            HighlightField highlightField = hit.getHighlightFields().get(request.getHighLightField());
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            Text[] fragments = highlightField.fragments();
            StringBuilder builder = new StringBuilder();
            for (Text fragment : fragments) {
                builder.append(fragment.string());
            }
            sourceAsMap.put(request.getHighLightField(), builder.toString());
        }

        return searchResponse;
    }

    @Override
    public SearchResponse structuredSearch(StructuredSearchRequest request) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.trackTotalHits(true);

        // 1、解析queryDSL
        String queryDsl = JSON.toJSONString(request.getQueryDsl());
        SearchModule searchModule = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
        NamedXContentRegistry namedXContentRegistry = new NamedXContentRegistry(searchModule.getNamedXContents());
        XContent xContent = XContentFactory.xContent(XContentType.JSON);
        XContentParser xContentParser = xContent.createParser(namedXContentRegistry, LoggingDeprecationHandler.INSTANCE, queryDsl);
        searchSourceBuilder.parseXContent(xContentParser);

        // 2、设置搜索分页参数
        int from = (request.getPageNum() - 1) * request.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(request.getPageSize());

        // 3、封装搜索请求
        SearchRequest searchRequest = new SearchRequest(request.getIndexName());
        searchRequest.source(searchSourceBuilder);

        // 4、查询elasticsearch
        return restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
    }
}
