package com.yu.yupicturebackend.api.imagesearch;

import java.util.List;

import com.yu.yupicturebackend.api.imagesearch.model.ImageSearchResult;
import com.yu.yupicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.yu.yupicturebackend.api.imagesearch.sub.GetImageListApi;
import com.yu.yupicturebackend.api.imagesearch.sub.GetImagePagerUrlApi;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImageSearchApiFacade {

    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePagerUrl = GetImagePagerUrlApi.getImagePagerUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePagerUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        String imageUrl = "http://i.lyu-cx.cn/space/1928091473354952706/2025-06-01_hK0MXHCdxKAIqsWB_thumbnail.jpg";
        List<ImageSearchResult> imageList = searchImage(imageUrl);
        System.out.println("结果列表" + imageList);
    }
}
