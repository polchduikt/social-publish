package com.socialpublish.common.web;

import org.springframework.validation.BindingResult;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static String firstFieldError(BindingResult bindingResult) {
        if (bindingResult.getFieldError() != null) {
            return bindingResult.getFieldError().getDefaultMessage();
        }
        return "Validation failed";
    }
}
