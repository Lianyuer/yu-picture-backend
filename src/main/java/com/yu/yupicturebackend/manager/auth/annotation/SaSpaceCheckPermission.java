package com.yu.yupicturebackend.manager.auth.annotation;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.yu.yupicturebackend.manager.auth.StpKit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 空间权限校验
 * <p> 可标注在函数、类上（效果等同于标注在此类的所有方法上）
 */
@SaCheckLogin(type = StpKit.SPACE_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SaSpaceCheckPermission {

}
