package com.yu.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum PictureReviewEnum {

    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);


    private final String text;

    private final int value;

    PictureReviewEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值得 value
     * @return 返回枚举
     */
    public static PictureReviewEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureReviewEnum pictureReviewEnum : PictureReviewEnum.values()) {
            if (pictureReviewEnum.value == value) {
                return pictureReviewEnum;
            }
        }
        return null;
    }
}
