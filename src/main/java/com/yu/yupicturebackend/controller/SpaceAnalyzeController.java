package com.yu.yupicturebackend.controller;

import com.yu.yupicturebackend.common.BaseResponse;
import com.yu.yupicturebackend.common.ResultUtils;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.model.dto.space.analyze.*;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.vo.space.analyze.*;
import com.yu.yupicturebackend.service.SpaceAnalyzeService;
import com.yu.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 获取空间分析数据
 */
@RestController
@RequestMapping("/space/analyze")
@Api(tags = "空间分析相关接口")
public class SpaceAnalyzeController {

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @Resource
    private UserService userService;

    /**
     * 获取空间使用分析数据接口
     *
     * @param spaceUsageAnalyzeRequest
     * @param request
     * @return
     */
    @GetMapping("/usage")
    @ApiOperation("空间使用分析接口")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse result = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 空间图片分类分析接口
     *
     * @param spaceCategoryAnalyzeRequest
     * @param request
     * @return
     */
    @GetMapping("/category")
    @ApiOperation("空间图片分类分析接口")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> result = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 空间图片标签分析接口
     *
     * @param spaceTagAnalyzeRequest
     * @param request
     * @return
     */
    @GetMapping("/tag")
    @ApiOperation("空间图片标签分析接口")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> result = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 空间图片大小分析接口
     *
     * @param spaceSizeAnalyzeRequest
     * @param request
     * @return
     */
    @GetMapping("/size")
    @ApiOperation("空间图片大小分析接口")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> result = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 用户上传行为分析接口
     *
     * @param spaceUserAnalyzeRequest
     * @param request
     * @return
     */
    @GetMapping("/user")
    @ApiOperation("用户上传行为分析接口")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
                                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> result = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }

}
