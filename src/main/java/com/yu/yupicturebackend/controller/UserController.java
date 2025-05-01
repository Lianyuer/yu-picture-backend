package com.yu.yupicturebackend.controller;

import com.sun.org.apache.xpath.internal.operations.Bool;
import com.yu.yupicturebackend.common.BaseResponse;
import com.yu.yupicturebackend.common.ResultUtils;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.model.dto.UserLoginDTO;
import com.yu.yupicturebackend.model.dto.UserRegisterDTO;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.vo.LoginUserVO;
import com.yu.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Data
@Slf4j
@RestController
@RequestMapping("/user")
@Api(tags = "用户相关接口")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 注册请求参数
     * @return 新用户 id
     */
    @PostMapping("/register")
    @ApiOperation("用户注册接口")
    public BaseResponse<Long> register(@RequestBody UserRegisterDTO userRegisterRequest) {
        Long userId = userService.register(userRegisterRequest);
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录请求参数
     * @param request          包含 http 请求信息的对象
     * @return 返回脱敏后的登录用户信息
     */
    @PostMapping("/login")
    @ApiOperation("用户登录接口")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginDTO userLoginRequest, HttpServletRequest request) {
        LoginUserVO loginUserVO = userService.login(userLoginRequest, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request 包含 http 请求信息的对象
     * @return 返回当前登录用户信息
     */
    @GetMapping("/get/login")
    @ApiOperation("获取当前登录用户信息接口")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     *
     * @param request 包含 http 请求信息的对象
     * @return 返回结果
     */
    @PostMapping("/logout")
    @ApiOperation("用户注销接口")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

}
