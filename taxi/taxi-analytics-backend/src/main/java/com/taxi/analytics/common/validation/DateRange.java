package com.taxi.analytics.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidatorImpl.class)
@Documented
public @interface DateRange {
    
    String message() default "日期范围无效";
    
    String startField();
    
    String endField();
    
    int maxDays() default 90;
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}