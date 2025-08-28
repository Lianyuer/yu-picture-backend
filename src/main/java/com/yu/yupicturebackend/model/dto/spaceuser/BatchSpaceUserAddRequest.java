package com.yu.yupicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 新增空间成员关联请求
 */
@Data
public class BatchSpaceUserAddRequest implements Serializable {

    private static final long serialVersionUID = -7046031085386419923L;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private List<Long> userIds;

    /**
     * 空间角色 viewer/editor/admin
     */
    private String spaceRole;

}
