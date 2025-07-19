package com.yu.yupicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间请求
 */
@Data
public class SpaceAddDTO implements Serializable {

    private static final long serialVersionUID = -7582739724670787303L;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版、1-专业版、2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-私有空间 1-团队空间
     */
    private Integer spaceType;

}
