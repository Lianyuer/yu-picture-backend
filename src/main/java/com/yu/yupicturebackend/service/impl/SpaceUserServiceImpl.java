package com.yu.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.mapper.SpaceUserMapper;
import com.yu.yupicturebackend.model.dto.spaceuser.BatchSpaceUserAddRequest;
import com.yu.yupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.yu.yupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.yu.yupicturebackend.model.entity.Space;
import com.yu.yupicturebackend.model.entity.SpaceUser;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.enums.SpaceRoleEnum;
import com.yu.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yu.yupicturebackend.model.vo.SpaceUserVO;
import com.yu.yupicturebackend.model.vo.SpaceVO;
import com.yu.yupicturebackend.model.vo.UserVO;
import com.yu.yupicturebackend.service.SpaceService;
import com.yu.yupicturebackend.service.SpaceUserService;
import com.yu.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lian
 * @description 针对表【space_user(空间用户关联表)】的数据库操作Service实现
 * @createDate 2025-07-24 09:22:54
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 新增空间成员
     *
     * @param spaceUserAddRequest 空间成员新增请求封装类
     * @return id
     */
    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validateSpaceUser(spaceUser, true);
        Long userId = spaceUser.getUserId();
        Long spaceId = spaceUser.getSpaceId();
        // 判断要新增的成员是否已存在
        boolean isExistedUser = this.lambdaQuery()
                .eq(SpaceUser::getUserId, userId)
                .eq(SpaceUser::getSpaceId, spaceId)
                .exists();
        ThrowUtils.throwIf(isExistedUser, ErrorCode.PARAMS_ERROR, "该成员已存在");
        // 数据库操作
        boolean isSuccess = this.save(spaceUser);
        ThrowUtils.throwIf(!isSuccess, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    /**
     * 批量添加成员到空间 (兼容批量删除的情况)
     *
     * @param batchSpaceUserAddRequest
     * @return
     */
    @Override
    public void batchAddSpaceUser(BatchSpaceUserAddRequest batchSpaceUserAddRequest) {
        ThrowUtils.throwIf(batchSpaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = batchSpaceUserAddRequest.getSpaceId();
        List<Long> userIds = batchSpaceUserAddRequest.getUserIds();
        // 判断要添加的成员数量,如果大于查询当前空间的成员数量,就是新增
        // 否则就需要先删除所有成员再新增
        // 删除之前需要先备份原先账号的空间角色
        // 查询当前空间的成员
        List<SpaceUser> currSpaceUser = this.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .select(SpaceUser::getUserId, SpaceUser::getSpaceRole).list();
        Map<Long, String> SpaceUserIdSpaceRoleMap = currSpaceUser.stream()
                .collect(Collectors.toMap(SpaceUser::getUserId, SpaceUser::getSpaceRole));
        if (userIds.size() <= currSpaceUser.size()) {
            QueryWrapper<SpaceUser> spaceUserQueryWrapper = new QueryWrapper<>();
            spaceUserQueryWrapper.eq("space_id", spaceId);
            boolean isRemove = this.remove(spaceUserQueryWrapper);
            ThrowUtils.throwIf(!isRemove, ErrorCode.OPERATION_ERROR);
        }
        List<SpaceUser> spaceUserList = userIds.stream()
                .map(id -> {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setUserId(id);
                    BeanUtils.copyProperties(batchSpaceUserAddRequest, spaceUser);
                    validateSpaceUser(spaceUser, true);
                    Long userId = spaceUser.getUserId();
                    String spaceRole = SpaceUserIdSpaceRoleMap.get(id);
                    spaceUser.setSpaceRole(spaceRole);
                    // 判断要新增的成员是否已存在
                    boolean isExistedUser = this.lambdaQuery()
                            .eq(SpaceUser::getUserId, userId)
                            .eq(SpaceUser::getSpaceId, spaceId)
                            .exists();
//                    ThrowUtils.throwIf(isExistedUser, ErrorCode.PARAMS_ERROR, "该成员已存在");
                    if (!isExistedUser) {
                        return spaceUser;
                    }
                    return null;
                })
                .filter(Objects::nonNull) // 过滤掉null值
                .collect(Collectors.toList());
        boolean isSuccess = this.saveBatch(spaceUserList);
        ThrowUtils.throwIf(!isSuccess, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 获取空间成员封装类
     *
     * @param spaceUser 空间成员对象
     * @return 空间成员封装类
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUserVO(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space);
            spaceUserVO.setSpaceVO(spaceVO);
        }
        return spaceUserVO;
    }

    /**
     * 获取空间成员封装类列表
     *
     * @param spaceUserList 空间成员信息列表
     * @return 空间成员封装类列表
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo)
                .collect(Collectors.toList());
        // 1、收集需要关联查询的用户 id 和空间 id
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 2、批量获取用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 3、关联用户信息和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            User user = null;
            // 填充用户信息
            Long userId = spaceUserVO.getUserId();
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUserVO(userService.getUserVO(user));
            Space space = null;
            // 填充空间信息
            Long spaceId = spaceUserVO.getSpaceId();
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpaceVO(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }

    /**
     * 获取查询对象
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return new QueryWrapper<>();
        }
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "space_id", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "space_role", spaceRole);
        return queryWrapper;
    }


    /**
     * 参数校验
     *
     * @param spaceUser 空间成员对象
     * @param add       是否是新增
     */
    @Override
    public void validateSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);

        // 创建时校验
        if (add) {
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "空间 id 不能为空");
            ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户 id 不能为空");
            ThrowUtils.throwIf(spaceRole == null, ErrorCode.PARAMS_ERROR, "用户角色不能为空");
            ThrowUtils.throwIf(userService.getById(userId) == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 私有空间不能关联成员
            ThrowUtils.throwIf(space.getSpaceType().equals(SpaceTypeEnum.PRIVATE.getValue()), ErrorCode.PARAMS_ERROR, "私有空间无法关联成员");
        }
        // 修改数据时，空间角色进行校验
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }
}




