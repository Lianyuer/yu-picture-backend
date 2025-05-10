package com.yu.yupicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.yu.yupicturebackend.annotation.AuthCheck;
import com.yu.yupicturebackend.common.BaseResponse;
import com.yu.yupicturebackend.common.DeleteRequest;
import com.yu.yupicturebackend.common.ResultUtils;
import com.yu.yupicturebackend.constant.UserConstant;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.model.dto.picture.PictureEditDTO;
import com.yu.yupicturebackend.model.dto.picture.PictureUpdateDTO;
import com.yu.yupicturebackend.model.dto.picture.PictureUploadDTO;
import com.yu.yupicturebackend.model.entity.Picture;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.enums.UserRoleEnum;
import com.yu.yupicturebackend.model.vo.PictureVO;
import com.yu.yupicturebackend.service.PictureService;
import com.yu.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/picture")
@Api(tags = "图片相关接口")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

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
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadDTO pictureUploadDTO,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadDTO, loginUser);
        return ResultUtils.success(pictureVO);
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
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateDTO pictureUpdateDTO) {
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
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
