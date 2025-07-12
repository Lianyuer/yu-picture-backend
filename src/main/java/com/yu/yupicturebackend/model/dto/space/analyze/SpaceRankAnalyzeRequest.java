package com.yu.yupicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用空间分析请求
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = -5356089869834969958L;

    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;

}
