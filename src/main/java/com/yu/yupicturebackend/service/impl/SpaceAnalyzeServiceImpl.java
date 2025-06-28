package com.yu.yupicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import com.yu.yupicturebackend.mapper.SpaceMapper;
import com.yu.yupicturebackend.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.yu.yupicturebackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.yu.yupicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.yu.yupicturebackend.model.entity.Picture;
import com.yu.yupicturebackend.model.entity.Space;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.yu.yupicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.yu.yupicturebackend.service.PictureService;
import com.yu.yupicturebackend.service.SpaceAnalyzeService;
import com.yu.yupicturebackend.service.SpaceService;
import com.yu.yupicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liany
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    // 校验空间分析权限
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 校验权限
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            ThrowUtils.throwIf(userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权访问");
        } else {
            // 私有空间权限校验
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    // 填充查询对象
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) { // 查询全空间，不需要拼接 spaceId 的条件
            return;
        }
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryPublic) { // 查询公共图库，需要拼接 spaceId 为 null 的条件
            queryWrapper.isNull("space_id");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("space_id", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        boolean queryPublic = spaceUsageAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceUsageAnalyzeRequest.isQueryAll();
        if (queryAll || queryPublic) {
            // 查询全空间或者公共图库
            // 仅管理员权限可查看
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权访问");
            // 统计公共图库的资源使用
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("pic_size");
            // 公共图库，条件：spaceId is null
            if (queryPublic) {
                queryWrapper.isNull("space_id");
            }
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Long ? (Long) result : 0).sum();
            long usedCount = pictureObjList.size();
            // 封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            // 公共图库无上限、无比例
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            // 查询指定空间
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            // 获取空间信息
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验权限，只有空间创建人和管理员才有权限访问
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            Long totalSize = space.getTotalSize();
            Long maxSize = space.getMaxSize();
            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setMaxSize(maxSize);
            double sizeUsageRatio = NumberUtil.round(totalSize * 100.0 / maxSize, 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);

            Long totalCount = space.getTotalCount();
            Long maxCount = space.getMaxCount();
            spaceUsageAnalyzeResponse.setUsedCount(totalCount);
            spaceUsageAnalyzeResponse.setMaxCount(maxCount);
            double countUsageRatio = NumberUtil.round(totalCount * 100.0 / maxCount, 2).doubleValue();
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 空间图片分类分析
     *
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 根据分析范围补充查询条件
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);

        queryWrapper.select("category",
                        "count(*) as count",
                        "sum(pic_size) as totalSize")
                .groupBy("category");

        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    long count = ((Number) result.get("count")).longValue();
                    long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                }).collect(Collectors.toList());
    }

}
