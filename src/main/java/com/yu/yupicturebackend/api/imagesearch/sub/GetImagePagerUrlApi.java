package com.yu.yupicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.yu.yupicturebackend.exception.BusinessException;
import com.yu.yupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址 - step 1
 */
@Slf4j
public class GetImagePagerUrlApi {

    /**
     * 获取以图搜图页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePagerUrl(String imageUrl) {
        // image https%3A%2F%2Fwww.codefather.cn%2Flogo.png
        // tn pc
        // from pc
        // image_source PC_UPLOAD_URL
        // sdkParams {}

        // 1、构造请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("image_source", "pc");
        formData.put("from", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        try {
            // 2、发送请求
            HttpResponse httpResponse = HttpRequest.post(url)
                    .form(formData)
                    .timeout(5000)
                    .execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 解析响应
            // {"status":0,"msg":"Success","data":{"url":"https://graph.baidu.com/s?card_key=\u0026entrance=GENERAL\u0026extUiData%5BisLogoShow%5D=1\u0026f=all\u0026isLogoShow=1\u0026session_id=44637329016591593\u0026sign=1262197ba6b9ad62a461901749218820\u0026tpl_from=pc","sign":"1262197ba6b9ad62a461901749218820"}}
            String body = httpResponse.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);

            // 3、处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            // 对 url 进行解码
            String rawUrl = (String) data.get("url");
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 url 为空
            if (StrUtil.isBlank(searchResultUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效的结果地址");
            }
            // 替换第一个部分
            String partToReplace = "?card_key=&client_app_id=&entrance=&f=general&jsup=&pageFrom=graph_upload_wise&";
            String replacement = "?card_key=&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&f=all&isLogoShow=1&";

            // 替换tn=pc部分
            String urlAfterFirstReplace = searchResultUrl.replace(partToReplace, replacement);
            String finalUrl = urlAfterFirstReplace.replace("&tn=pc", "&tpl_from=pc");
            return finalUrl;
        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        // 测试以图搜图
        String url = "http://i.lyu-cx.cn/space/1928091473354952706/2025-06-01_hK0MXHCdxKAIqsWB_thumbnail.jpg";
        String searchResultUrl = getImagePagerUrl(url);
        System.out.println("搜索成功，结果 url：" + searchResultUrl);
    }
}
