package com.yu.yupicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 空间成员权限配置
 */
@Data
public class SpaceUserAuthConfig implements Serializable {

    private static final long serialVersionUID = 3270567783313606870L;

    /**
     * 权限列表
     */
    List<SpaceUserPermission> permissions;

    /**
     * 角色列表
     */
    List<SpaceUserRole> roles;

}
