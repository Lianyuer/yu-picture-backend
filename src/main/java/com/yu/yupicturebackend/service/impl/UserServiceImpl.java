package com.yu.yupicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.model.dto.UserRegisterDTO;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.service.UserService;
import com.yu.yupicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.regex.Pattern;

/**
 * @author liany
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2025-04-30 11:09:48
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 注册
     *
     * @param userRegisterRequest 注册请求参数
     * @return 新用户 id
     */
    @Override
    public Long register(UserRegisterDTO userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 1、参数校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, userPassword), ErrorCode.PARAMS_ERROR);
        // 2、账号密码长度和特殊字符校验
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号不能少于4位");
        ThrowUtils.throwIf(userPassword.length() < 6, ErrorCode.PARAMS_ERROR, "密码不能少于6位");
        ThrowUtils.throwIf(Pattern.compile("^[a-zA-Z0-9]+$\n").matcher(userAccount).matches(), ErrorCode.PARAMS_ERROR, "账号不能出现特殊字符");
        // 2、两次输入的密码是否一致校验
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次密码输入不一致");
        // 3、判断账号是否已经被注册
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        User esixtUser = this.getOne(queryWrapper);
        ThrowUtils.throwIf(esixtUser != null, ErrorCode.PARAMS_ERROR, "账号已存在");
        // 4、密码加密
        String SALT = "yu_picture_salt_by_lian_yu";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean isSaved = this.save(user);
        ThrowUtils.throwIf(!isSaved, ErrorCode.SYSTEM_ERROR);
        return user.getId();
    }
}




