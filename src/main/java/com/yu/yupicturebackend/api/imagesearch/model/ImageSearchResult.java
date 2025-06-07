package com.yu.yupicturebackend.api.imagesearch.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片搜索结果
 */
@Data
public class ImageSearchResult implements Serializable {

    private static final long serialVersionUID = -7538466356843088330L;

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;

}
