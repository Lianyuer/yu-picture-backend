package com.yu.yupicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yu.yupicturebackend.constant.UserConstant;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.model.dto.user.*;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.vo.LoginUserVO;
import com.yu.yupicturebackend.model.vo.UserVO;
import com.yu.yupicturebackend.service.UserService;
import com.yu.yupicturebackend.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author liany
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2025-04-30 11:09:48
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 获取加密后的密码
     *
     * @param password 原密码
     * @return 返回加密后的密码
     */
    @Override
    public String getEncryptPassword(String password) {
        // 加盐，混淆密码
        String SALT = "yu_picture_salt_by_lian_yu";
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }

    /**
     * 获取脱敏后的登录用户信息
     *
     * @param user 原登录用户信息
     * @return 返回脱敏后的登录用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 原用户信息
     * @return 返回脱敏后的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的用户信息列表
     *
     * @param userList 原用户信息列表
     * @return 返回脱敏后的用户信息列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取查询用户的包装对象
     *
     * @param userQueryDTO 用户查询请求封装类
     * @return 返回 QueryWrapper 对象
     */
    public QueryWrapper<User> getQueryWrapper(UserQueryDTO userQueryDTO) {
        ThrowUtils.throwIf(userQueryDTO == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = userQueryDTO.getId();
        String userAccount = userQueryDTO.getUserAccount();
        String userName = userQueryDTO.getUserName();
        String userProfile = userQueryDTO.getUserProfile();
        String userRole = userQueryDTO.getUserRole();
        String sortField = userQueryDTO.getSortField();
        String sortOrder = userQueryDTO.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "user_account", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "user_name", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "user_profile", userProfile);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "user_role", userRole);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

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
        ThrowUtils.throwIf(!Pattern.compile("^[a-zA-Z0-9]+$").matcher(userAccount).matches(), ErrorCode.PARAMS_ERROR, "账号不能出现特殊字符");
        // 2、两次输入的密码是否一致校验
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次密码输入不一致");
        // 3、判断账号是否已经被注册
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        User esixtUser = this.getOne(queryWrapper);
        ThrowUtils.throwIf(esixtUser != null, ErrorCode.PARAMS_ERROR, "账号已存在");
        // 4、密码加密
        String encryptPassword = this.getEncryptPassword(userPassword);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean isSaved = this.save(user);
        ThrowUtils.throwIf(!isSaved, ErrorCode.SYSTEM_ERROR);
        return user.getId();
    }

    /**
     * 登录
     *
     * @param userLoginRequest 登录请求参数
     * @param request          包含 http 请求信息的对象
     * @return 脱敏后的登录用户信息
     */
    @Override
    public LoginUserVO login(UserLoginDTO userLoginRequest, HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        // 1、参数校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "账号或密码错误");
        // 2、密码加密
        String encryptPassword = this.getEncryptPassword(userPassword);
        // 3、查询数据库中的用户是否存在，不存在则抛异常
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("user_account", userAccount);
        userQueryWrapper.eq("user_password", encryptPassword);
        User user = this.getOne(userQueryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "账号或密码错误");
        // 4、数据脱敏
        LoginUserVO loginUserVO = getLoginUserVO(user);
        // 5、保存用户登录态
        request.getSession().setAttribute(UserConstant.LOGIN_USER_STATE, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request 包含 http 请求信息的对象
     * @return 返回当前登录用户信息
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 判断是否已经登录
        LoginUserVO currentUser = (LoginUserVO) request.getSession().getAttribute(UserConstant.LOGIN_USER_STATE);
        ThrowUtils.throwIf(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);

        // 从数据库中查询（追求性能的话，可以直接返回上述结果）
        User user = this.getById(currentUser.getId());
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        return user;
    }

    /**
     * 用户注销
     *
     * @param request 包含 http 请求信息的对象
     * @return 返回成功与否的结果
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        LoginUserVO loginUser = (LoginUserVO) request.getSession().getAttribute(UserConstant.LOGIN_USER_STATE);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.OPERATION_ERROR, "未登录");
        request.getSession().removeAttribute(UserConstant.LOGIN_USER_STATE);
        return true;
    }

    /**
     * 创建用户
     *
     * @param userAddDTO 用户创建请求参数
     * @return 返回创建用户的 id
     */
    @Override
    public Long addUser(UserAddDTO userAddDTO) {
        ThrowUtils.throwIf(userAddDTO == null || StrUtil.hasEmpty(userAddDTO.getUserAccount(),
                userAddDTO.getUserRole()), ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddDTO, user);
        // 默认密码
        final String DEFAULT_PASSWORD = "123456";
        String encryptPassword = this.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        User existUser = this.getOne(new QueryWrapper<User>().eq("user_Account", userAddDTO.getUserAccount()));
        ThrowUtils.throwIf(existUser != null, ErrorCode.PARAMS_ERROR, "账号已存在");
        boolean isSaved = this.save(user);
        ThrowUtils.throwIf(!isSaved, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    /**
     * 更新用户
     *
     * @param userUpdateDTO 更新用户请求参数
     * @return 返回结果 true or false
     */
    @Override
    public Boolean updateUser(UserUpdateDTO userUpdateDTO) {
        ThrowUtils.throwIf(userUpdateDTO == null || StrUtil.hasEmpty(userUpdateDTO.getUserRole()), ErrorCode.PARAMS_ERROR);
        User existUser = this.getOne(new QueryWrapper<User>().eq("id", userUpdateDTO.getId()));
        ThrowUtils.throwIf(existUser == null, ErrorCode.NOT_FOUND_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userUpdateDTO, user);
        boolean isUpdated = this.updateById(user);
        ThrowUtils.throwIf(!isUpdated, ErrorCode.OPERATION_ERROR);
        return true;
    }

}




