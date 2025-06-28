package com.yu.yupicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.yupicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.yu.yupicturebackend.model.entity.Space;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;

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
}
