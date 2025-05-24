package com.yu.yupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.yu.yupicturebackend.config.CosClientConfig;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.manager.CosManager;
import com.yu.yupicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传文件
     *
     * @param inputSource      输入源
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1、校验图片
        validatePicture(inputSource);
        // 2、图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        // 文件格式：日期_uuid.文件后缀
        String uploadFileName = String.format(("%s_%s.%s"), DateUtil.formatDate(new Date()),
                uuid, FileUtil.getSuffix(originalFilename));
        // 自己拼接文件上传路径，而不是使用原始文件名，可以增强安全性
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            // 3、上传临时文件，获取文件到服务器
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源
            processFile(inputSource, file);
            // 4、上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 5、获取图片信息对象，封装返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 获取到图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                // 获取压缩之后得到的文件信息
                CIObject compressedciObject = objectList.get(0);
                // 缩略图默认等于压缩图
                CIObject thumbnailciObject = compressedciObject;
                // 有生成缩略图，才得到缩略图
                if (objectList.size() > 1) {
                    thumbnailciObject = objectList.get(1);
                }
                // 封装压缩图的返回结果
                return buildResult(originalFilename, compressedciObject, thumbnailciObject);
            }
            return buildResult(imageInfo, uploadPath, originalFilename, file);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6、清理临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 处理输入源并生成本地临时文件
     *
     * @param inputSource 输入源
     */
    protected abstract void validatePicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     *
     * @param inputSource 输入源
     * @return 原始文件名
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 校验输入源（本地文件或 URL）
     *
     * @param inputSource 输入源
     * @param file
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 封装返回结果
     *
     * @param originalFilename   原始文件名
     * @param compressedciObject 压缩后的对象
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressedciObject, CIObject thumbnailciObject) {
        // 计算宽高
        int picWidth = compressedciObject.getWidth();
        int picHeight = compressedciObject.getHeight();
        double picScale = NumberUtil.round((picWidth * 1.0 / picHeight), 2).doubleValue();
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedciObject.getKey());
        uploadPictureResult.setName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(compressedciObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedciObject.getFormat());
        // 设置缩略图
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailciObject.getKey());
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     *
     * @param imageInfo        对象存储返回的图片信息
     * @param uploadPath
     * @param originalFilename
     * @param file
     * @return
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String uploadPath, String originalFilename, File file) {
        // 计算宽高
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round((picWidth * 1.0 / picHeight), 2).doubleValue();
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        return uploadPictureResult;
    }

    /**
     * 清理临时文件
     *
     * @param file 文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("临时文件删除失败，文件路径: {}", file.getAbsolutePath());
        }
    }

}
