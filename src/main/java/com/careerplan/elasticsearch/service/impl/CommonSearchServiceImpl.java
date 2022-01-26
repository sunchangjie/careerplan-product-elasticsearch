package com.careerplan.elasticsearch.service.impl;

import com.careerplan.elasticsearch.dto.AutoCompleteRequest;
import com.careerplan.elasticsearch.dto.RecommendWhenMissingRequest;
import com.careerplan.elasticsearch.dto.SpellingCorrectionRequest;
import com.careerplan.elasticsearch.service.CommonSearchService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestion;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestion;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 通用查询服务实现类
 *
 * @author zhonghuashishan
 */
@Service
public class CommonSearchServiceImpl implements CommonSearchService {

    private static final String MY_SUGGEST = "my_suggest";

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public List<String> autoComplete(AutoCompleteRequest request) throws IOException {

        // 1、构建 CompletionSuggestion 条件
        CompletionSuggestionBuilder completionSuggestionBuilder = SuggestBuilders.completionSuggestion(request.getFieldName());
        completionSuggestionBuilder.prefix(request.getText());
        completionSuggestionBuilder.skipDuplicates(true);
        completionSuggestionBuilder.size(request.getCount());

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
        searchSourceBuilder.suggest(new SuggestBuilder().addSuggestion(MY_SUGGEST, completionSuggestionBuilder));

        // 2、封装搜索请求
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(request.getIndexName());
        searchRequest.source(searchSourceBuilder);

        // 3、查询elasticsearch
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        System.out.println("searchRequest:"+searchRequest);
        System.out.println("completionSuggestionBuilder:"+completionSuggestionBuilder);

        // 4、获取响应中的补全的词的列表
        CompletionSuggestion completionSuggestion = searchResponse.getSuggest().getSuggestion(MY_SUGGEST);
        List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getEntries().get(0).getOptions();

        List<String> result = new ArrayList<>();
        for (CompletionSuggestion.Entry.Option option : options) {
            result.add(option.getText().string());
        }

        return result;
    }

    @Override
    public String spellingCorrection(SpellingCorrectionRequest request) throws IOException {

        // 1、构建  PhraseSuggestion 条件
        PhraseSuggestionBuilder phraseSuggestionBuilder = new PhraseSuggestionBuilder(request.getFieldName());
        phraseSuggestionBuilder.text(request.getText());
         // 只取一条
        phraseSuggestionBuilder.size(1);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
        searchSourceBuilder.suggest(new SuggestBuilder().addSuggestion(MY_SUGGEST, phraseSuggestionBuilder));

        // 2、封装搜索请求
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(request.getIndexName());
        searchRequest.source(searchSourceBuilder);

        // 3、查询elasticsearch
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        // 4、获取响应中纠错后的词
        PhraseSuggestion phraseSuggestion = searchResponse.getSuggest().getSuggestion(MY_SUGGEST);
        List<PhraseSuggestion.Entry.Option> options = phraseSuggestion.getEntries().get(0).getOptions();

        return Optional.ofNullable(options).filter(e -> !e.isEmpty()).map(e -> e.get(0)).map(e -> e.getText().string()).orElse("");
    }
}
