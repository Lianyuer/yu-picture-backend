package com.yu.yupicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yu.yupicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.yu.yupicturebackend.manager.auth.model.SpaceUserRole;
import com.yu.yupicturebackend.model.entity.Picture;
import com.yu.yupicturebackend.model.entity.Space;
import com.yu.yupicturebackend.model.entity.SpaceUser;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.enums.SpaceRoleEnum;
import com.yu.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yu.yupicturebackend.service.SpaceUserService;
import com.yu.yupicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 空间成员权限管理类，
 * 可加载配置文件到对象，
 * 并提供根据角色获取权限列表的方法
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    private static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据空间成员角色获取权限列表
     *
     * @param spaceUserRole
     * @return
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return new ArrayList<>();
        }
        // 找到匹配的角色
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> r.getKey().equals(spaceUserRole))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        return role.getPermissions();
    }

    /**
     * 获取权限列表
     *
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, Picture picture, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser) || picture.getUserId().equals(loginUser.getId())) {
                return ADMIN_PERMISSIONS;
            }
            return new ArrayList<>();
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 判断空间类型
        // 私有空间：仅空间创建者和管理员有操作权限
        // 团队空间：根据当前登录用户在团队中的角色返回相应权限，若用户不在团队中，则返回空权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

}
