package com.yu.yupicturebackend.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用的删除请求类
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 156646757335236659L;

    @ApiModelProperty(value = "用户id", required = true)
    private Long id;

}
