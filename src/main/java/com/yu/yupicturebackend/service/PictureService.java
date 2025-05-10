package com.yu.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.yupicturebackend.model.dto.picture.PictureQueryDTO;
import com.yu.yupicturebackend.model.dto.picture.PictureUploadDTO;
import com.yu.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.yupicturebackend.model.entity.User;
import com.yu.yupicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

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
     * @param picture
     */
    void validatePicture(Picture picture);

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param pictureUploadDTO 上传图片请求的参数封装类
     * @param loginUser        当前登录用户
     * @return 返回上传的图片封装信息
     */
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadDTO pictureUploadDTO, User loginUser);
}
