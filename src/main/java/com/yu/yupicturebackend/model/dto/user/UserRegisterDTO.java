package com.yu.yupicturebackend.model.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterDTO implements Serializable {

    private static final long serialVersionUID = 5258375412281220951L;

    /**
     * 账号
     */
    @ApiModelProperty(value = "账号", required = true)
    private String userAccount;

    /**
     * 密码
     */
    @ApiModelProperty(value = "密码", required = true)
    private String userPassword;

    /**
     * 校验密码
     */
    @ApiModelProperty(value = "校验密码", required = true)
    private String checkPassword;

}
