package com.yu.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 以图搜图请求
 */
@Data
public class SearchPictureByPictureDTO implements Serializable {

    private static final long serialVersionUID = -8966733560131344272L;

    /**
     * 图片 id
     */
    private Long pictureId;

}
