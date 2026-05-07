package com.taxi.analytics.common.validation;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateRangeValidatorImpl implements ConstraintValidator<DateRange, Object> {
    
    private String startField;
    private String endField;
    private int maxDays;
    
    @Override
    public void initialize(DateRange annotation) {
        this.startField = annotation.startField();
        this.endField = annotation.endField();
        this.maxDays = annotation.maxDays();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);
            String startDateStr = (String) beanWrapper.getPropertyValue(startField);
            String endDateStr = (String) beanWrapper.getPropertyValue(endField);
            
            if (startDateStr == null || endDateStr == null) {
                return true; // 由@NotNull处理
            }
            
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            
            // 开始日期不能大于结束日期
            if (startDate.isAfter(endDate)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("开始日期不能大于结束日期")
                    .addConstraintViolation();
                return false;
            }
            
            // 日期范围不能超过最大天数
            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            if (days > maxDays) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("日期范围不能超过" + maxDays + "天")
                    .addConstraintViolation();
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}