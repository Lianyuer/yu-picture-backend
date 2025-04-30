package com.yu.yupicturebackend.controller;

import com.yu.yupicturebackend.common.BaseResponse;
import com.yu.yupicturebackend.common.ResultUtils;
import com.yu.yupicturebackend.model.dto.UserRegisterDTO;
import com.yu.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

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
}
