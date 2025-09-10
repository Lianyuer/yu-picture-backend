package com.yu.yupicturebackend.manager.websocket.disruptor;

import com.yu.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yu.yupicturebackend.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditEvent {

    /**
     * 图片编辑请求消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private UserVO user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
