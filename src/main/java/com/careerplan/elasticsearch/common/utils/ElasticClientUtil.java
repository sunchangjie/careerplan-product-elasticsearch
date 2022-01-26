package com.careerplan.elasticsearch.common.utils;

import com.careerplan.elasticsearch.common.constant.StringPoolConstant;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * es client的工具类
 *
 * @author zhonghuashishan
 */
public class ElasticClientUtil {

    /**
     * 获取高亮内容正则
     */
    private static final Pattern PATTERN = Pattern.compile("(?<=<span style=\"color:red\">).*?(?=</span>)");

    /**
     * 获取分页查询资源构建器
     *
     * @param page 页码
     * @param size 每页大小
     * @return 结果
     */
    public static SearchSourceBuilder builderPageSearchBuilder(Integer page, Integer size) {
        // 给page默认值
        if (page == null && size != null) {
            page = 1;
        }

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        if (page != null && size != null) {
            sourceBuilder.from((page - 1) * size);
            sourceBuilder.size(size);
        }
        return sourceBuilder;
    }

    /**
     * 构建高亮查询结果
     *
     * @param hit           搜索命中的结果
     * @param searchField   查询字段
     * @param searchContent 查询内容
     * @return              高亮字符串
     */
    public static String buildHighlightResult(SearchHit hit, String searchField, String searchContent) {
        if (StringUtils.hasLength(searchContent)) {
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField highlightField = highlightFields.get(searchField);
            if (highlightField == null){
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Text fragment : highlightField.getFragments()) {
                sb.append(fragment.toString());
            }
            return sb.toString();
        }
        return null;
    }


    /**
     * 添加高亮显示查询
     *
     * @param sourceBuilder 搜索资源构建器
     * @param searchField   高亮字段
     */
    public static void sourceBuilderAddHighlight(SearchSourceBuilder sourceBuilder, String searchField) {
        // 高亮显示
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        HighlightBuilder.Field highlightTitle = new HighlightBuilder.Field(searchField);
        highlightBuilder.field(highlightTitle);

        // 设置自定义高亮标签
        highlightBuilder.preTags(StringPoolConstant.PRE_TAG);
        highlightBuilder.postTags(StringPoolConstant.POST_TAG);

        sourceBuilder.highlighter(highlightBuilder);
    }

    /**
     * 获取高亮文本内容
     *
     * @param highlightString 高亮字符串
     * @return 去除掉标签之后的文本内容
     */
    public static String getHighlightContent(String highlightString){
        Matcher matcher = PATTERN.matcher(highlightString);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

}