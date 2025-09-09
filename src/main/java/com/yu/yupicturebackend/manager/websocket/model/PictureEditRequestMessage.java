package com.yu.yupicturebackend.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 图片编辑请求消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditRequestMessage implements Serializable {

    private static final long serialVersionUID = 4734110281467898032L;

    /**
     * 消息类型，例如 “ENTER_EDIT”，“EXIT_EDIT”，“EDIT_ACTION”
     */
    private String type;

    /**
     * 执行的编辑操作
     */
    private String editAction;
}
