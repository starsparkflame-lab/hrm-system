package com.cow.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在控制器方法上，用于自定义日志标题与业务类型。
 * <p>
 * 未标注的写操作（POST/PUT/DELETE）也会被切面自动记录，
 * 标题由“模块名 + 请求方式”推导，详见 OperLogAspect。
 *
 * @Author : cow
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 日志标题，如“管理员登录”
     */
    String title();

    /**
     * 业务类型 0其他 1新增 2修改 3删除 4登录 5登出 6导出 7导入
     */
    int businessType() default 0;
}
