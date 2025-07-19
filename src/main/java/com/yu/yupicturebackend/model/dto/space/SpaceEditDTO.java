package com.yu.yupicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间编辑请求
 */
@Data
public class SpaceEditDTO implements Serializable {

    private static final long serialVersionUID = -1432552189407680240L;

    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间类型：0-私有空间 1-团队空间
     */
    private Integer spaceType;

}
