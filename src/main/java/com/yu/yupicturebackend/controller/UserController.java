package com.yu.yupicturebackend.controller;

import com.yu.yupicturebackend.annotation.AuthCheck;
import com.yu.yupicturebackend.common.BaseResponse;
import com.yu.yupicturebackend.common.ResultUtils;
import com.yu.yupicturebackend.constant.UserConstant;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.model.dto.user.UserAddDTO;
import com.yu.yupicturebackend.model.dto.user.UserLoginDTO;
import com.yu.yupicturebackend.model.dto.user.UserRegisterDTO;
import com.yu.yupicturebackend.model.dto.user.UserUpdateDTO;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.vo.LoginUserVO;
import com.yu.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 创建用户
     *
     * @param userAddDTO 用户创建参数
     * @return 返回创建用户的 id
     */
    @PostMapping("/add")
    @ApiOperation("创建用户接口")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddDTO userAddDTO) {
        Long userId = userService.addUser(userAddDTO);
        return ResultUtils.success(userId);
    }

    /**
     * 更新用户
     *
     * @param userUpdateDTO 更新用户请求参数
     * @return 返回结果 true or false
     */
    @PostMapping("/update")
    @ApiOperation("更新用户接口")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateDTO userUpdateDTO) {
        Boolean isUpdated = userService.updateUser(userUpdateDTO);
        return ResultUtils.success(isUpdated);
    }

}
