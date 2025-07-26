package com.yu.yupicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增空间成员关联请求
 */
@Data
public class SpaceUserAddRequest implements Serializable {

    private static final long serialVersionUID = 5068815570679981302L;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色 viewer/editor/admin
     */
    private String spaceRole;

}
