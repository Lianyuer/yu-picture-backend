package com.yu.yupicturebackend.model.dto.user;

import com.yu.yupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryDTO extends PageRequest implements Serializable {

    private static final long serialVersionUID = -4514726068501566781L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user / admin
     */
    private String userRole;

}
