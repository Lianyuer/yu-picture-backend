package com.yu.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.yupicturebackend.model.dto.space.SpaceAddDTO;
import com.yu.yupicturebackend.model.dto.space.SpaceQueryDTO;
import com.yu.yupicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.vo.SpaceVO;

/**
 * @author liany
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-05-27 21:56:13
 */
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     *
     * @param spaceAddDTO
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddDTO spaceAddDTO, User loginUser);

    /**
     * 校验空间
     *
     * @param space 空间对象
     * @param add   是否是创建时检验
     */
    void validateSpace(Space space, boolean add);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @return
     */
    SpaceVO getSpaceVO(Space space);

    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage);

    /**
     * 获取查询对象
     *
     * @param spaceQueryDTO
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryDTO spaceQueryDTO);


    /**
     * 根据空间级别填充空间对象
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

}
