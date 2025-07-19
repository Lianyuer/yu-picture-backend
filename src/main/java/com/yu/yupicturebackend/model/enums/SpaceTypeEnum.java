package com.yu.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间类型枚举
 */
@Getter
public enum SpaceTypeEnum {

    PRIVATE("私有", 0 ),
    TEAM("团队", 1 );


    private final String text;

    private final int value;

    /**
     * @param text     文本
     * @param value    值
     */
    SpaceTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值得 value
     * @return 返回枚举
     */
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceTypeEnum spaceTypeEnum : SpaceTypeEnum.values()) {
            if (spaceTypeEnum.value == value) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}
