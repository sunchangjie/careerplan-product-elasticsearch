package com.careerplan.elasticsearch.dto;

import com.careerplan.elasticsearch.common.enums.QueryTypeEnum;
import lombok.Data;

/**
 * 商品信息
 *
 * @author zhonghuashishan
 */
@Data
public class ProductSearchDto {

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 排序字段
     */
    private String orderField;

    /**
     * 排序类型 asc desc
     */
    private String orderType;

    /**
     * 查询类型
     * {@link QueryTypeEnum}
     */
    private Integer queryType;

    /**
     * 页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;

}
