package com.yu.yupicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.yu.yupicturebackend.constant.UserConstant;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.model.entity.Picture;
import com.yu.yupicturebackend.model.entity.Space;
import com.yu.yupicturebackend.model.entity.SpaceUser;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.enums.SpaceRoleEnum;
import com.yu.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yu.yupicturebackend.service.PictureService;
import com.yu.yupicturebackend.service.SpaceService;
import com.yu.yupicturebackend.service.SpaceUserService;
import com.yu.yupicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 权限登录类型校验, 登录类型不是 space, 则返回空权限
        if (!Objects.equals(loginType, StpKit.SPACE.loginType)) {
            return new ArrayList<>();
        }
        // 管理员权限, 表示权限校验通过
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 从请求中获取上下文对象, 检查上下文对象字段如果全部为空，表示查看公共图库，返回管理员权限
        SpaceUserAuthContext spaceUserAuthContext = getContextByRequest();
        if (isAllFieldsNull(spaceUserAuthContext)) {
            return ADMIN_PERMISSIONS;
        }
        // 获取当前登录用户信息
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.LOGIN_USER_STATE);
        Long userId = loginUser.getId();
        // 优先从上下文对象中获取 spaceUser 对象
        SpaceUser spaceUser = spaceUserAuthContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 如果有 spaceUserId, 必然是团队空间，通过数据库查询 SpaceUser 对象
        Long spaceUserId = spaceUserAuthContext.getSpaceUserId();
        if (spaceUserId != null) {
            SpaceUser tempSpaceUser = spaceUserService.getById(spaceUserId);
            ThrowUtils.throwIf(tempSpaceUser == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            // 校验当前登录用户是否属于该空间
            Long spaceId = tempSpaceUser.getSpaceId();
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            // 如果不属于，返回空权限
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 属于，则返回在空间的对应角色权限
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 获取 Space 对象并处理
        Long spaceId = spaceUserAuthContext.getSpaceId();
        if (spaceId == null) {
            // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = spaceUserAuthContext.getPictureId();
            // 图片 id 也没有，则默认通过权限校验
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            spaceId = picture.getSpaceId();
            // 公共图库, 仅本人和管理员可操作
            if (spaceId == 0) {
                if (picture.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    // 不是自己的图片，仅可查看
                    return Collections.singletonList(SpaceRoleEnum.VIEWER.getValue());
                }
            }

        }
        // 获取空间信息, 判断空间类型
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        Integer spaceType = space.getSpaceType();
        // 私有空间，仅空间创建者和管理员有操作权限，其他用户无权限
        if (spaceType.equals(SpaceTypeEnum.PRIVATE.getValue())) {
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        }
        // 团队空间，查询登录用户在该空间的角色，并返回对应权限码，如果登录用户不属于该空间，返回空权限
        else if (spaceType.equals(SpaceTypeEnum.TEAM.getValue())) {
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            } else {
                return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
            }
        }
        return new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 从请求中获取上下文对象
     *
     * @return
     */
    private SpaceUserAuthContext getContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        // 兼容 get 和 post
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestURI = request.getRequestURI();
            String partUrI = requestURI.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partUrI, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }

    /**
     * 判断对象字段是否全部为空
     *
     * @param obj
     * @return
     */
    private Boolean isAllFieldsNull(Object obj) {
        if (obj == null) {
            return true;
        }
        // 利用反射判断对象字段是否为空
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(obj.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(obj, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }

}
