package com.socialpublish.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class LinkedInUrnValidator implements ConstraintValidator<LinkedInUrn, String> {

    private static final Pattern URN_PATTERN =
            Pattern.compile("^urn:li:(person|organization):[A-Za-z0-9_-]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return URN_PATTERN.matcher(value.trim()).matches();
    }
}
