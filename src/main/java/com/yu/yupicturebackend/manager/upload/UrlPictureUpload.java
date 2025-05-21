package com.yu.yupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import com.yu.yupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * URL 图片上传
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    protected void validatePicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 1、参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        try {
            // 验证 URL格式
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址不正确");
        }

        // 验证 URL 协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://")) && !fileUrl.
                        startsWith(("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        HttpResponse response = null;
        try {
            // 3、head请求校验图片是否存在
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }

            // 4、校验图片类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_FORMAT_LIST = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(contentType.toLowerCase()), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 5、校验图片大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong((contentLengthStr));
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超管2MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        }
        // 6、释放资源
        finally {
            response.close();
        }
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.getName(fileUrl);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }
}
