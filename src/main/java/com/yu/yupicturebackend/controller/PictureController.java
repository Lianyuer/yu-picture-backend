package com.yu.yupicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yu.yupicturebackend.annotation.AuthCheck;
import com.yu.yupicturebackend.api.aliyunai.AliYunAiApi;
import com.yu.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yu.yupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yu.yupicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.yu.yupicturebackend.api.imagesearch.model.ImageSearchResult;
import com.yu.yupicturebackend.common.BaseResponse;
import com.yu.yupicturebackend.common.DeleteRequest;
import com.yu.yupicturebackend.common.ResultUtils;
import com.yu.yupicturebackend.constant.UserConstant;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.model.dto.picture.*;
import com.yu.yupicturebackend.model.entity.Picture;
import com.yu.yupicturebackend.model.entity.Space;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.enums.PictureReviewEnum;
import com.yu.yupicturebackend.model.vo.PictureTagCategoryVO;
import com.yu.yupicturebackend.model.vo.PictureVO;
import com.yu.yupicturebackend.service.PictureService;
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
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/picture")
@Api(tags = "图片相关接口")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

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
     * 上传图片
     *
     * @param multipartFile    文件
     * @param pictureUploadDTO 上传图片请求的参数封装类
     * @param request          包含 http 请求信息的对象
     * @return 返回上传的图片封装信息
     */
    @PostMapping("/upload")
    @ApiOperation("上传图片接口")
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadDTO pictureUploadDTO,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadDTO, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过 URL 上传图片 (可重新上传)
     *
     * @param pictureUploadDTO 上传图片请求的参数封装类
     * @param request          包含 http 请求信息的对象
     * @return 返回上传的图片封装信息
     */
    @PostMapping("/upload/url")
    @ApiOperation("通过 URL 上传图片接口")
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadDTO pictureUploadDTO,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadDTO.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadDTO, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 批量抓取图片
     *
     * @param pictureUploadByBatchDTO
     * @param request
     * @return 成功创建的图片数
     */
    @PostMapping("/upload/batch")
    @ApiOperation("批量抓取图片接口")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchDTO pictureUploadByBatchDTO,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchDTO, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest 删除文件请求
     * @param request       携带 http 请求信息的对象
     * @return 返回 true or false
     */
    @PostMapping("/delete")
    @ApiOperation("删除图片接口")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片 (仅管理员)
     *
     * @param pictureUpdateDTO 图片更新请求
     * @return 返回 true or false
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员更新图片接口")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateDTO pictureUpdateDTO, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(pictureUpdateDTO == null || pictureUpdateDTO.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateDTO, picture);
        // 标签 json 格式转换
        List<String> tags = pictureUpdateDTO.getTags();
        String tagsStr = JSONUtil.toJsonStr(tags);
        picture.setTags(tagsStr);
        // 数据校验
        pictureService.validatePicture(picture);
        // 判断图片是否存在
        Long pictureId = picture.getId();
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补全审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片 (给用户使用)
     *
     * @param pictureEditDTO 编辑用户请求
     * @param request        携带 http 请求信息的对象
     * @return 返回 true or false
     */
    @PostMapping("/edit")
    @ApiOperation("用户编辑图片接口")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditDTO pictureEditDTO, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(pictureEditDTO == null || pictureEditDTO.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditDTO, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片 (仅管理员)
     *
     * @param id 图片 id
     * @return 返回图片信息
     */
    @GetMapping("/get")
    @ApiOperation("管理员根据 id 获取图片接口")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片封装信息
     *
     * @param id 图片 id
     * @return 返回图片封装信息
     */
    @GetMapping("/get/vo")
    @ApiOperation("根据 id 获取图片封装信息接口")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser, picture);
        }
        return ResultUtils.success(pictureService.getPictureVO(picture));
    }

    /**
     * 分页获取图片列表 (仅管理员)
     *
     * @param pictureQueryDTO 分页查询请求
     * @return
     */
    @PostMapping("/list/page")
    @ApiOperation("管理员分页查询图片列表接口")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<PictureVO>> listPictureByPage(@RequestBody PictureQueryDTO pictureQueryDTO) {
        int current = pictureQueryDTO.getCurrent();
        int size = pictureQueryDTO.getSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size)
                , pictureService.getQueryWrapper(pictureQueryDTO));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage));
    }

    /**
     * 分页查询图片列表 (封装类)
     *
     * @param pictureQueryDTO 分页查询请求
     * @return
     */
    @PostMapping("/list/page/vo")
    @ApiOperation("分页查询图片列表封装类")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryDTO pictureQueryDTO,
                                                             HttpServletRequest request) {
        int current = pictureQueryDTO.getCurrent();
        int size = pictureQueryDTO.getSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能查看已过审的数据
        pictureQueryDTO.setReviewStatus(PictureReviewEnum.PASS.getValue());
        // 空间权限校验
        Long spaceId = pictureQueryDTO.getSpaceId();
        if (spaceId == null) {
            // 公开图库
            // 普通用户默认只能看到审核通过的数据
            pictureQueryDTO.setReviewStatus(PictureReviewEnum.PASS.getValue());
            pictureQueryDTO.setNullSpaceId(true);
        } else {
            // 私有空间
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
            // 私有空间默认查询所有审核状态的图片
            pictureQueryDTO.setReviewStatus(null);
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryDTO));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage));
    }

    /**
     * 分页查询图片列表 (封装类)(使用 redis 分布式缓存)
     *
     * @param pictureQueryDTO 分页查询请求
     * @return
     */
//    @PostMapping("/list/page/vo/cache")
//    @ApiOperation("使用缓存分页查询图片列表封装类")
//    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryDTO pictureQueryDTO) {
//        int current = pictureQueryDTO.getCurrent();
//        int size = pictureQueryDTO.getSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 普通用户默认只能查看已过审的数据
//        pictureQueryDTO.setReviewStatus(PictureReviewEnum.PASS.getValue());
//
//        // 查询缓存，若缓存中没有， 再查数据库
//        // 构建缓存的 key
//        String queryCondition = JSONUtil.toJsonStr(pictureQueryDTO);
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        String redisKey = String.format("yuzipicture:listPictureVOByPage:%s", hashKey);
//        // 创建操作对象，操作 Redis，从缓存中查询
//        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
//        String cachedValue = valueOps.get(redisKey);
//        if (cachedValue != null) {
//            // 如果缓存命中，缓存结果
//            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
//            return ResultUtils.success(cachedPage);
//        }
//        // 缓存未命中
//        // 查询数据库
//        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
//                pictureService.getQueryWrapper(pictureQueryDTO));
//        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);
//
//        // 存入 Redis 缓存
//        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//        // 设置缓存过期时间，5 - 10 分钟过期，防止缓存雪崩
//        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
//        valueOps.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
//
//        return ResultUtils.success(pictureVOPage);
//    }

    /**
     * 分页查询图片列表 (封装类)(使用 caffeine 本地缓存)
     *
     * @param pictureQueryDTO 分页查询请求
     * @return
     */
//    @PostMapping("/list/page/vo/cache")
//    @ApiOperation("使用缓存分页查询图片列表封装类")
//    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryDTO pictureQueryDTO) {
//        int current = pictureQueryDTO.getCurrent();
//        int size = pictureQueryDTO.getSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 普通用户默认只能查看已过审的数据
//        pictureQueryDTO.setReviewStatus(PictureReviewEnum.PASS.getValue());
//
//        // 查询缓存，若缓存中没有， 再查数据库
//        // 构建缓存的 key
//        String queryCondition = JSONUtil.toJsonStr(pictureQueryDTO);
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        String cacheKey = String.format("listPictureVOByPage:%s", hashKey);
//        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
//        if (cachedValue != null) {
//            // 如果缓存命中，缓存结果
//            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
//            return ResultUtils.success(cachedPage);
//        }
//        // 缓存未命中
//        // 查询数据库
//        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
//                pictureService.getQueryWrapper(pictureQueryDTO));
//        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);
//
//        // 存入 缓存
//        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//        LOCAL_CACHE.put(cacheKey, cacheValue);
//        return ResultUtils.success(pictureVOPage);
//    }

    /**
     * 分页查询图片列表 (封装类)(使用多级缓存 redis 分布式缓存 + Caffeine 本地缓存)
     *
     * @param pictureQueryDTO 分页查询请求
     * @return
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    @ApiOperation("使用缓存分页查询图片列表封装类")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryDTO pictureQueryDTO) {
        int current = pictureQueryDTO.getCurrent();
        int size = pictureQueryDTO.getSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能查看已过审的数据
        pictureQueryDTO.setReviewStatus(PictureReviewEnum.PASS.getValue());

        // 查询缓存，若缓存中没有， 再查数据库
        // 构建缓存的 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryDTO);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("yuzipicture:listPictureVOByPage:%s", hashKey);

        // 1、查询本地缓存 (Caffeine)
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 本地缓存命中，返回
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachePage);
        }

        // 2、查询分布式缓存 (Redis)
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        cachedValue = valueOps.get(cacheKey);
        if (cachedValue != null) {
            // 如果命中 Redis，存入本地缓存并返回
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }

        // 缓存均未命中
        // 3、查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryDTO));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);

        // 4、更新缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 更新本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        // 更新 Redis 缓存，设置过期时间为 5 分钟
        valueOps.set(cacheKey, cacheValue, 5, TimeUnit.MINUTES);

        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 获取图片标签分类列表
     *
     * @return
     */
    @GetMapping("/tag_category")
    @ApiOperation("获取图片标签分类列表接口")
    public BaseResponse<PictureTagCategoryVO> listPictureTagCategory() {
        PictureTagCategoryVO pictureTagCategoryVO = new PictureTagCategoryVO();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategoryVO.setTagList(tagList);
        pictureTagCategoryVO.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategoryVO);
    }

    /**
     * 图片审核
     *
     * @param pictureReviewDTO 图片审核参数
     * @param request          包含 http 请求信息的对象
     */
    @PostMapping("/review")
    @ApiOperation("图片审核接口")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> pictureReview(PictureReviewDTO pictureReviewDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.pictureReview(pictureReviewDTO, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 以图搜图
     *
     * @return
     */
    @PostMapping("/search/picture")
    @ApiOperation("以图搜图接口")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureDTO searchPictureByPictureDTO) {
        ThrowUtils.throwIf(searchPictureByPictureDTO == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureDTO.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getThumbnailUrl());
        return ResultUtils.success(resultList);
    }

    /**
     * 根据颜色查询图片列表
     *
     * @param searchPictureByColorDTO
     * @param request
     * @return
     */
    @PostMapping("/search/color")
    @ApiOperation("根据颜色查询图片接口")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorDTO searchPictureByColorDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long spaceId = searchPictureByColorDTO.getSpaceId();
        String picColor = searchPictureByColorDTO.getPicColor();
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchDTO
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    @ApiOperation("批量编辑图片接口")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchDTO pictureEditByBatchDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.PictureEditByBatch(pictureEditByBatchDTO, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @PostMapping("out_painting/create_task")
    @ApiOperation("创建 AI 扩图任务接口")
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                                                                    HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     *
     * @param taskId
     * @return
     */
    @GetMapping("out_painting/get_task")
    @ApiOperation("查询 AI 扩图任务接口")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(taskId == null, ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }
}
