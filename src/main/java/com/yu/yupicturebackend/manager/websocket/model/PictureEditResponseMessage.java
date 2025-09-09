package com.yu.yupicturebackend.manager.websocket.model;

import com.yu.yupicturebackend.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 图片编辑响应消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditResponseMessage implements Serializable {

    private static final long serialVersionUID = -4176786337118876155L;

    /**
     * 消息类型，例如 “INFO”，“ERROR”，“ENTER_EDIT”，“EXIT_EDIT”，“EDIT_ACTION”
     */
    private String type;

    /**
     * 消息
     */
    private String message;

    /**
     * 执行的编辑操作
     */
    private String editAction;

    /**
     * 用户信息
     */
    private UserVO userVO;
}
