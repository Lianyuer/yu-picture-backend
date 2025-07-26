package com.yu.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yu.yupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.yu.yupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.yu.yupicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.yupicturebackend.model.vo.SpaceUserVO;

import java.util.List;

/**
 * @author lian
 * @description 针对表【space_user(空间用户关联表)】的数据库操作Service
 * @createDate 2025-07-24 09:22:54
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 新增空间成员
     *
     * @param spaceUserAddRequest
     * @return 关系表中成功新增的记录的 id
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 获取空间成员封装类
     *
     * @param spaceUser 空间成员对象
     * @return 空间成员封装类
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 获取空间成员封装类列表
     *
     * @param spaceUserList 空间成员信息列表
     * @return 空间成员封装类列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询对象
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 参数校验
     *
     * @param spaceUser 空间成员对象
     * @param add       是否是新增
     */
    void validateSpaceUser(SpaceUser spaceUser, boolean add);
}
