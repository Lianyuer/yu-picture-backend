package com.yu.yupicturebackend.utils;

import java.util.Random;

public class RandomNicknameGenerator {

    /**
     * 生成随机6位大写英文字母昵称
     *
     * @return 6位大写字母组成的随机字符串
     */
    public static String generateRandomNickname() {
        Random random = new Random();
        StringBuilder nickname = new StringBuilder();

        // 生成6个随机大写字母
        for (int i = 0; i < 6; i++) {
            char randomChar = (char) (random.nextInt(26) + 'A');
            nickname.append(randomChar);
        }

        return "鱼友_" + nickname.toString();
    }

//    // 测试方法
//    public static void main(String[] args) {
//        // 生成10个随机昵称作为示例
//        for (int i = 0; i < 10; i++) {
//            System.out.println("随机昵称 " + (i+1) + ": " + generateRandomNickname());
//        }
//    }
}