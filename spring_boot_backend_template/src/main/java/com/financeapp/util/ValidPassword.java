package com.financeapp.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for password complexity requirements.
 * 
 * Password must:
 * - Be 8-100 characters long
 * - Contain at least one uppercase letter (A-Z)
 * - Contain at least one lowercase letter (a-z)
 * - Contain at least one digit (0-9)
 * - Contain at least one special character (!@#$%^&*-_+=)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface ValidPassword {
    
    String message() default "Password must be 8-100 characters and contain at least one: uppercase letter, lowercase letter, digit, and special character (!@#$%^&*-_+=)";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
