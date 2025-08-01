package com.yu.yupicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 空间成员角色类
 */
@Data
public class SpaceUserRole implements Serializable {

    private static final long serialVersionUID = -5006033470015196151L;

    /**
     * 角色键
     */
    private String key;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 角色描述
     */
    private String description;

}
