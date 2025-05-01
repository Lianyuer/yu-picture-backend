package com.yu.yupicturebackend.service;

import com.yu.yupicturebackend.model.dto.UserLoginDTO;
import com.yu.yupicturebackend.model.dto.UserRegisterDTO;
import com.yu.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.yupicturebackend.model.vo.LoginUserVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author liany
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2025-04-30 11:09:48
 */
public interface UserService extends IService<User> {

    /**
     * 获取加密后的密码
     *
     * @param password 原密码
     * @return 返回加密后的密码
     */
    String getEncryptPassword(String password);

    /**
     * 获取脱敏后的登录用户信息
     *
     * @param user 原登录用户信息
     * @return 返回脱敏后的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 注册
     *
     * @param userRegisterRequest 注册请求参数
     * @return 新用户 id
     */
    Long register(UserRegisterDTO userRegisterRequest);

    /**
     * 登录
     *
     * @param userLoginRequest 登录请求参数
     * @param request          包含 http 请求信息的对象
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO login(UserLoginDTO userLoginRequest, HttpServletRequest request);

}
