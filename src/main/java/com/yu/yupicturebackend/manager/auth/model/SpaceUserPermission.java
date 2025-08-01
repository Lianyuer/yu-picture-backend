package com.yu.yupicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间成员权限类
 */
@Data
public class SpaceUserPermission implements Serializable {

    private static final long serialVersionUID = 7609591728194601001L;

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

}
