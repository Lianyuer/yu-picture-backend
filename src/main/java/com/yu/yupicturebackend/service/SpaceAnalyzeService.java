package com.yu.yupicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.yupicturebackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.yu.yupicturebackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.yu.yupicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.yu.yupicturebackend.model.entity.Space;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.yu.yupicturebackend.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.yu.yupicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;

import java.util.List;

/**
 * @author liany
 */
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 空间图片分类分析
     *
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 空间图片标签分析
     *
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);
}
