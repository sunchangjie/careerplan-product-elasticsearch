package com.careerplan.elasticsearch.controller;

import com.careerplan.elasticsearch.dto.RecommendWhenMissingRequest;
import com.careerplan.elasticsearch.dto.SpellingCorrectionRequest;
import com.careerplan.elasticsearch.service.CommonSearchService;
import com.careerplan.elasticsearch.common.core.JsonResult;
import com.careerplan.elasticsearch.dto.AutoCompleteRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 通用搜索控制类
 *
 * @author zhonghuashishan
 */
@RestController
@RequestMapping("/api/common")
public class CommonSearchController {

    /**
     * 通用服务组件
     */
    @Autowired
    private CommonSearchService commonSearchService;

    /**
     * 输入内容自动补全接口
     */
    @GetMapping("/autoComplete")
    public JsonResult autoComplete(@RequestBody AutoCompleteRequest request) throws IOException {
        List<String> completedWords = commonSearchService.autoComplete(request);
        return JsonResult.buildSuccess(completedWords);
    }

    /**
     * 输入内容拼写纠错接口
     */
    @GetMapping("/spellingCorrection")
    public JsonResult spellingCorrection(@RequestBody SpellingCorrectionRequest request) throws IOException {
        String correctedWord = commonSearchService.spellingCorrection(request);
        return JsonResult.buildSuccess(correctedWord);
    }
}
