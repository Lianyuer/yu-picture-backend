package com.yu.yupicturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求类
 */
@Data
public class PageRequest implements Serializable {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int size = 10;

    /**
     * 排序字段
     */
    private String sortField = "create_time";

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = "descend";

}
