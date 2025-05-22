package com.yu.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量图片上传请求
 */
@Data
public class PictureUploadByBatchDTO implements Serializable {

    private static final long serialVersionUID = -2855969101013163449L;

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

}
