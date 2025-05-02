package com.yu.yupicturebackend.model.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginDTO implements Serializable {

    private static final long serialVersionUID = 2240106282231983748L;

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

}
