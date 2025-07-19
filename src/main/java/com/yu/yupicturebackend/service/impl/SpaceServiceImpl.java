package com.yu.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.model.dto.space.SpaceAddDTO;
import com.yu.yupicturebackend.model.dto.space.SpaceQueryDTO;
import com.yu.yupicturebackend.model.entity.Space;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.enums.SpaceLevelEnum;
import com.yu.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yu.yupicturebackend.model.vo.SpaceVO;
import com.yu.yupicturebackend.model.vo.UserVO;
import com.yu.yupicturebackend.service.SpaceService;
import com.yu.yupicturebackend.mapper.SpaceMapper;
import com.yu.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liany
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-05-27 21:56:13
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public long addSpace(SpaceAddDTO spaceAddDTO, User loginUser) {
        // 1. 填充参数默认值
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddDTO, space);
        // 默认值
        if (StrUtil.isBlank(spaceAddDTO.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddDTO.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (spaceAddDTO.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 2. 校验参数
        this.validateSpace(space, true);
        // 3. 校验权限，非管理员只能创建普通级别的空间
        if (spaceAddDTO.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 4. 控制同一用户只能创建一个私有空间
        // 针对用户进行加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 判断空间是否已经存在
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, spaceAddDTO.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建一个");
                // 写入数据库
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                // 返回新的空间 id
                return space.getId();
            });
            // 返回结果是包装类，可以做一些处理
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    @Override
    public void validateSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 创建时校验
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            ThrowUtils.throwIf(spaceType == null, ErrorCode.PARAMS_ERROR, "空间类型不能为空");
        }
        // 修改数据时，空间名称进行校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 修改数据时，空间级别进行校验
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        // 修改数据时，空间类型进行校验
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 获取关联的用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        // 分页结果为空，则返回空的分页列表
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());

        // 1、 查询关联用户信息
        Set<Long> userIdsSet = spaceVOList.stream().map(SpaceVO::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdsSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2、填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryDTO spaceQueryDTO) {
        ThrowUtils.throwIf(spaceQueryDTO == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        // 从对象中取值
        Long id = spaceQueryDTO.getId();
        Long userId = spaceQueryDTO.getUserId();
        String spaceName = spaceQueryDTO.getSpaceName();
        Integer spaceLevel = spaceQueryDTO.getSpaceLevel();
        Integer spaceType = spaceQueryDTO.getSpaceType();
        String sortField = spaceQueryDTO.getSortField();
        String sortOrder = spaceQueryDTO.getSortOrder();
        // 拼接查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "user_id", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "space_name", spaceName);
        queryWrapper.eq(ObjUtil.isNotNull(spaceLevel), "space_level", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotNull(spaceType), "space_type", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        Long userId = space.getUserId();
        ThrowUtils.throwIf(!loginUser.getId().equals(userId) && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
    }

}




