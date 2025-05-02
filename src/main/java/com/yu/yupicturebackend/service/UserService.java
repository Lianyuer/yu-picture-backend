package com.yu.yupicturebackend.service;

import com.yu.yupicturebackend.common.DeleteRequest;
import com.yu.yupicturebackend.model.dto.user.UserAddDTO;
import com.yu.yupicturebackend.model.dto.user.UserLoginDTO;
import com.yu.yupicturebackend.model.dto.user.UserRegisterDTO;
import com.yu.yupicturebackend.model.dto.user.UserUpdateDTO;
import com.yu.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.yupicturebackend.model.vo.LoginUserVO;
import com.yu.yupicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
     * 获取脱敏后的用户信息
     *
     * @param user 原用户信息
     * @return 返回脱敏后的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息列表
     *
     * @param userList 原用户信息列表
     * @return 返回脱敏后的用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

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

    /**
     * 获取当前登录用户信息
     *
     * @param request 包含 http 请求信息的对象
     * @return 返回获取当前登录用户信息
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request 包含 http 请求信息的对象
     * @return 返回成功与否的结果
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 创建用户
     *
     * @param userAddDTO 用户创建参数
     * @return 返回创建用户的 id
     */
    Long addUser(UserAddDTO userAddDTO);

    /**
     * 更新用户
     *
     * @param userUpdateDTO 更新用户请求参数
     * @return 返回结果 true or false
     */
    Boolean updateUser(UserUpdateDTO userUpdateDTO);

    /**
     * 删除用户
     *
     * @param deleteRequest 删除用户请求参数
     * @return 返回删除结果 true or false
     */
    Boolean deleteUser(DeleteRequest deleteRequest);
}
