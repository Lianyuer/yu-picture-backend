package com.yu.yupicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.yu.yupicturebackend.annotation.AuthCheck;
import com.yu.yupicturebackend.common.BaseResponse;
import com.yu.yupicturebackend.common.DeleteRequest;
import com.yu.yupicturebackend.common.ResultUtils;
import com.yu.yupicturebackend.constant.UserConstant;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.model.dto.picture.*;
import com.yu.yupicturebackend.model.entity.Picture;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.enums.PictureReviewEnum;
import com.yu.yupicturebackend.model.enums.UserRoleEnum;
import com.yu.yupicturebackend.model.vo.PictureTagCategoryVO;
import com.yu.yupicturebackend.model.vo.PictureVO;
import com.yu.yupicturebackend.service.PictureService;
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
import java.util.Arrays;
import java.util.Date;
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
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
        // 查询数据库中是否存在图片
        Long pictureId = deleteRequest.getId();
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或者管理员可进行删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = pictureService.removeById(pictureId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
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
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditDTO, picture);
        // 标签 list => String
        List<String> tags = pictureEditDTO.getTags();
        String tagsStr = JSONUtil.toJsonStr(tags);
        picture.setTags(tagsStr);
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        pictureService.validatePicture(picture);
        // 校验图片是否存在
        Long pictureId = picture.getId();
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 不是创建图片的用户或者不是管理员，则无权限编辑
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 补全审核参数
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
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
    public BaseResponse<PictureVO> getPictureVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
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
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryDTO pictureQueryDTO) {
        int current = pictureQueryDTO.getCurrent();
        int size = pictureQueryDTO.getSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能查看已过审的数据
        pictureQueryDTO.setReviewStatus(PictureReviewEnum.PASS.getValue());
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryDTO));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage));
    }

    /**
     * 分页查询图片列表 (封装类)(使用缓存)
     *
     * @param pictureQueryDTO 分页查询请求
     * @return
     */
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
        String redisKey = String.format("yuzipicture:listPictureVOByPage:%s", hashKey);
        // 创建操作对象，操作 Redis，从缓存中查询
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        String cachedValue = valueOps.get(redisKey);
        if (cachedValue != null) {
            // 如果缓存命中，缓存结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 缓存未命中
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryDTO));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);

        // 存入 Redis 缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 设置缓存过期时间，5 - 10 分钟过期，防止缓存雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        valueOps.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

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
}
