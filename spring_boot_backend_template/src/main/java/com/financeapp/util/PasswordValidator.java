package com.financeapp.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for password complexity requirements.
 * Checks for: length (8-100), uppercase, lowercase, digit, and special character.
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    private static final String PASSWORD_PATTERN = 
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*\\-_+=]).{8,100}$";
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null values are handled by @NotNull annotation
        if (value == null) {
            return true;
        }
        
        return value.matches(PASSWORD_PATTERN);
    }
}
