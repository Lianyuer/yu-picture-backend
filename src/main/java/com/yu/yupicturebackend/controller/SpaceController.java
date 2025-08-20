package com.yu.yupicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yu.yupicturebackend.annotation.AuthCheck;
import com.yu.yupicturebackend.common.BaseResponse;
import com.yu.yupicturebackend.common.DeleteRequest;
import com.yu.yupicturebackend.common.ResultUtils;
import com.yu.yupicturebackend.constant.UserConstant;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.manager.auth.SpaceUserAuthManager;
import com.yu.yupicturebackend.model.dto.space.*;
import com.yu.yupicturebackend.model.entity.Space;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.enums.SpaceLevelEnum;
import com.yu.yupicturebackend.model.vo.SpaceVO;
import com.yu.yupicturebackend.service.SpaceService;
import com.yu.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/space")
@Api(tags = "空间相关接口")
public class SpaceController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 本地缓存 Caffeine
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024) // 分配初始容量，提高启动效率
            .maximumSize(10_000L) // 最大 10000 条
            // 缓存 5分钟后移除
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * 新增空间接口
     *
     * @param spaceAddDTO
     * @param request
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("新增空间接口")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddDTO spaceAddDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddDTO, loginUser);
        return ResultUtils.success(newId);
    }

    /**
     * 删除空间
     *
     * @param deleteRequest 删除文件请求
     * @param request       携带 http 请求信息的对象
     * @return 返回 true or false
     */
    @PostMapping("/delete")
    @ApiOperation("删除空间接口")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 查询数据库中是否存在空间
        Long spaceId = deleteRequest.getId();
        Space oldSpace = spaceService.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或者管理员可进行删除
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = spaceService.removeById(spaceId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新空间 (仅管理员)
     *
     * @param spaceUpdateDTO 空间更新请求
     * @return 返回 true or false
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员更新空间接口")
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateDTO spaceUpdateDTO, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceUpdateDTO == null || spaceUpdateDTO.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 将实体类与 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateDTO, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validateSpace(space, false);
        // 判断空间是否存在
        Long spaceId = space.getId();
        Space oldSpace = spaceService.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间 (给用户使用)
     *
     * @param spaceEditDTO 编辑用户请求
     * @param request      携带 http 请求信息的对象
     * @return 返回 true or false
     */
    @PostMapping("/edit")
    @ApiOperation("用户编辑空间接口")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditDTO spaceEditDTO, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceEditDTO == null || spaceEditDTO.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditDTO, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 设置编辑时间
        space.setEditTime(new Date());
        // 数据校验
        spaceService.validateSpace(space, false);
        // 校验空间是否存在
        Long spaceId = space.getId();
        Space oldSpace = spaceService.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 仅本人或管理员可编辑
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取空间 (仅管理员)
     *
     * @param id 空间 id
     * @return 返回空间信息
     */
    @GetMapping("/get")
    @ApiOperation("管理员根据 id 获取空间接口")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间封装信息
     *
     * @param id 空间 id
     * @return 返回空间封装信息
     */
    @GetMapping("/get/vo")
    @ApiOperation("根据 id 获取空间封装信息接口")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        SpaceVO spaceVO = spaceService.getSpaceVO(space);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, null, loginUser);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表 (仅管理员)
     *
     * @param spaceQueryDTO 分页查询请求
     * @return
     */
    @PostMapping("/list/page")
    @ApiOperation("管理员分页查询空间列表接口")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SpaceVO>> listSpaceByPage(@RequestBody SpaceQueryDTO spaceQueryDTO) {
        int current = spaceQueryDTO.getCurrent();
        int size = spaceQueryDTO.getSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size)
                , spaceService.getQueryWrapper(spaceQueryDTO));
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage));
    }

    /**
     * 分页查询空间列表 (封装类)
     *
     * @param spaceQueryDTO 分页查询请求
     * @return
     */
    @PostMapping("/list/page/vo")
    @ApiOperation("分页查询空间列表封装类")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryDTO spaceQueryDTO) {
        int current = spaceQueryDTO.getCurrent();
        int size = spaceQueryDTO.getSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryDTO));
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage));
    }

    /**
     * 分页查询空间列表 (封装类)(使用 redis 分布式缓存)
     *
     * @param spaceQueryDTO 分页查询请求
     * @return
     */
//    @PostMapping("/list/page/vo/cache")
//    @ApiOperation("使用缓存分页查询空间列表封装类")
//    public BaseResponse<Page<SpaceVO>> listSpaceVOByPageWithCache(@RequestBody SpaceQueryDTO spaceQueryDTO) {
//        int current = spaceQueryDTO.getCurrent();
//        int size = spaceQueryDTO.getSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 普通用户默认只能查看已过审的数据
//        spaceQueryDTO.setReviewStatus(SpaceReviewEnum.PASS.getValue());
//
//        // 查询缓存，若缓存中没有， 再查数据库
//        // 构建缓存的 key
//        String queryCondition = JSONUtil.toJsonStr(spaceQueryDTO);
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        String redisKey = String.format("yuzispace:listSpaceVOByPage:%s", hashKey);
//        // 创建操作对象，操作 Redis，从缓存中查询
//        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
//        String cachedValue = valueOps.get(redisKey);
//        if (cachedValue != null) {
//            // 如果缓存命中，缓存结果
//            Page<SpaceVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
//            return ResultUtils.success(cachedPage);
//        }
//        // 缓存未命中
//        // 查询数据库
//        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
//                spaceService.getQueryWrapper(spaceQueryDTO));
//        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(spacePage);
//
//        // 存入 Redis 缓存
//        String cacheValue = JSONUtil.toJsonStr(spaceVOPage);
//        // 设置缓存过期时间，5 - 10 分钟过期，防止缓存雪崩
//        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
//        valueOps.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
//
//        return ResultUtils.success(spaceVOPage);
//    }

    /**
     * 分页查询空间列表 (封装类)(使用 caffeine 本地缓存)
     *
     * @param spaceQueryDTO 分页查询请求
     * @return
     */
//    @PostMapping("/list/page/vo/cache")
//    @ApiOperation("使用缓存分页查询空间列表封装类")
//    public BaseResponse<Page<SpaceVO>> listSpaceVOByPageWithCache(@RequestBody SpaceQueryDTO spaceQueryDTO) {
//        int current = spaceQueryDTO.getCurrent();
//        int size = spaceQueryDTO.getSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 普通用户默认只能查看已过审的数据
//        spaceQueryDTO.setReviewStatus(SpaceReviewEnum.PASS.getValue());
//
//        // 查询缓存，若缓存中没有， 再查数据库
//        // 构建缓存的 key
//        String queryCondition = JSONUtil.toJsonStr(spaceQueryDTO);
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        String cacheKey = String.format("listSpaceVOByPage:%s", hashKey);
//        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
//        if (cachedValue != null) {
//            // 如果缓存命中，缓存结果
//            Page<SpaceVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
//            return ResultUtils.success(cachedPage);
//        }
//        // 缓存未命中
//        // 查询数据库
//        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
//                spaceService.getQueryWrapper(spaceQueryDTO));
//        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(spacePage);
//
//        // 存入 缓存
//        String cacheValue = JSONUtil.toJsonStr(spaceVOPage);
//        LOCAL_CACHE.put(cacheKey, cacheValue);
//        return ResultUtils.success(spaceVOPage);
//    }

    /**
     * 分页查询空间列表 (封装类)(使用多级缓存 redis 分布式缓存 + Caffeine 本地缓存)
     *
     * @param spaceQueryDTO 分页查询请求
     * @return
     */
    @PostMapping("/list/page/vo/cache")
    @ApiOperation("使用缓存分页查询空间列表封装类")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPageWithCache(@RequestBody SpaceQueryDTO spaceQueryDTO) {
        int current = spaceQueryDTO.getCurrent();
        int size = spaceQueryDTO.getSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 查询缓存，若缓存中没有， 再查数据库
        // 构建缓存的 key
        String queryCondition = JSONUtil.toJsonStr(spaceQueryDTO);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("yuzipicture:listSpaceVOByPage:%s", hashKey);

        // 1、查询本地缓存 (Caffeine)
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 本地缓存命中，返回
            Page<SpaceVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachePage);
        }

        // 2、查询分布式缓存 (Redis)
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        cachedValue = valueOps.get(cacheKey);
        if (cachedValue != null) {
            // 如果命中 Redis，存入本地缓存并返回
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<SpaceVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }

        // 缓存均未命中
        // 3、查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryDTO));
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(spacePage);

        // 4、更新缓存
        String cacheValue = JSONUtil.toJsonStr(spaceVOPage);
        // 更新本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        // 更新 Redis 缓存，设置过期时间为 5 分钟
        valueOps.set(cacheKey, cacheValue, 5, TimeUnit.MINUTES);

        return ResultUtils.success(spaceVOPage);
    }

    /**
     * 获取空间级别列表，便于前端展示
     *
     * @return
     */
    @GetMapping("/list/level")
    @ApiOperation("获取空间级别对象列表接口")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                )).collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

}
