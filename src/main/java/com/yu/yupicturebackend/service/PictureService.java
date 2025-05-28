package com.yu.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.yupicturebackend.common.DeleteRequest;
import com.yu.yupicturebackend.model.dto.picture.*;
import com.yu.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.vo.PictureVO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author liany
 * @description 针对表【picture(图片表)】的数据库操作Service
 * @createDate 2025-05-09 23:29:20
 */
public interface PictureService extends IService<Picture> {

    /**
     * 获取查询对象
     *
     * @param pictureQueryDTO
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryDTO);

    /**
     * 获取图片包装类（单条）
     *
     * @param picture
     * @return
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片包装类（分页）
     *
     * @param picturePage
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 校验图片
     *
     * @param picture
     */
    void validatePicture(Picture picture);

    /**
     * 填充审核参数
     *
     * @param picture   图片
     * @param loginUser 当前登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 上传图片
     *
     * @param inputSource      文件输入源
     * @param pictureUploadDTO 上传图片请求的参数封装类
     * @param loginUser        当前登录用户
     * @return 返回上传的图片封装信息
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadDTO pictureUploadDTO, User loginUser);

    /**
     * 图片审核
     *
     * @param pictureReviewDTO 图片审核参数
     * @param loginUser        当前登录用户
     */
    void pictureReview(PictureReviewDTO pictureReviewDTO, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchDTO
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO, User loginUser);

    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     *
     * @param pictureEditDTO
     * @param loginUser
     */
    void editPicture(PictureEditDTO pictureEditDTO, User loginUser);

    /**
     * 校验空间图片的权限
     *
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);
}
