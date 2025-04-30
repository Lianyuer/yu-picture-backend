package com.yu.yupicturebackend.service;

import com.yu.yupicturebackend.model.dto.UserRegisterDTO;
import com.yu.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author liany
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2025-04-30 11:09:48
 */
public interface UserService extends IService<User> {

    /**
     * 注册
     *
     * @param userRegisterRequest 注册请求参数
     * @return 新用户 id
     */
    Long register(UserRegisterDTO userRegisterRequest);
}
