package com.socialpublish.common.validation;

import com.socialpublish.common.model.PlatformType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PlatformListValidator implements ConstraintValidator<PlatformList, List<String>> {

    private static final Set<PlatformType> ALLOWED = EnumSet.allOf(PlatformType.class);

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        for (String token : value) {
            if (token == null || token.isBlank()) {
                return false;
            }
            String normalized = token.trim();
            String platformPrefix = normalized.contains(":")
                    ? normalized.substring(0, normalized.indexOf(':'))
                    : normalized;
            try {
                PlatformType platform = PlatformType.valueOf(platformPrefix.toUpperCase(Locale.ROOT));
                if (!ALLOWED.contains(platform)) {
                    return false;
                }
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        return true;
    }
}
