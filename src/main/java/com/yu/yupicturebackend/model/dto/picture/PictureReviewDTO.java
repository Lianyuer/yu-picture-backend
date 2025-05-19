package com.yu.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片更新请求
 */
@Data
public class PictureReviewDTO implements Serializable {

    private static final long serialVersionUID = -1008728576794039909L;

    /**
     * id
     */
    private Long id;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

}
