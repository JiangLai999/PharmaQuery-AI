package com.pharmacy.aspect;

import java.lang.annotation.*;

/**
 * 审计日志注解 (论文 3.4.2)
 * 标注在Controller方法上，自动记录操作行为
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {
    /** 操作描述 */
    String value();
}
