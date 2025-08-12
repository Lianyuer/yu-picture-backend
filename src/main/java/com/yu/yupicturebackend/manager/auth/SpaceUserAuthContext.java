package com.yu.yupicturebackend.manager.auth;

import com.yu.yupicturebackend.model.entity.Picture;
import com.yu.yupicturebackend.model.entity.Space;
import com.yu.yupicturebackend.model.entity.SpaceUser;
import lombok.Data;

/**
 * 空间权限上下文对象
 * 表示用户在特定空间内的授权上下文，包括关联的图片、空间和用户信息
 */
@Data
public class SpaceUserAuthContext {

    /**
     * 临时参数，不同请求对应的 id 可能不同
     */
    private Long id;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 空间成员 id
     */
    private Long spaceUserId;

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 空间信息
     */
    private Space space;

    /**
     * 空间成员信息
     */
    private SpaceUser spaceUser;

    /**
     * 图片信息
     */
    private Picture picture;

}
