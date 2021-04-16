package com.test.myspring.annonation;

import java.lang.annotation.*;

/**
 * @author wangsheng
 * @version V1.0
 * @ClassName: RequestMapping
 * @Description: TODO
 * @Date 2021/4/14 15:04
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping1 {
    String[] value() default {};
}
