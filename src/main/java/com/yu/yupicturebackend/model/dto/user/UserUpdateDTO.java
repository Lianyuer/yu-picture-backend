package com.yu.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateDTO implements Serializable {

    private static final long serialVersionUID = 5426959981678559082L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user / admin
     */
    private String userRole;

}
