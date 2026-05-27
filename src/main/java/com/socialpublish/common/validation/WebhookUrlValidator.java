package com.socialpublish.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.util.Locale;

public class WebhookUrlValidator implements ConstraintValidator<WebhookUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.trim();
        if (normalized.contains("...")) {
            // masked secret placeholder accepted to preserve existing value
            return true;
        }

        try {
            URI uri = URI.create(normalized);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            String hostLower = host.toLowerCase(Locale.ROOT);
            return hostLower.endsWith("discord.com")
                    || hostLower.endsWith("discordapp.com")
                    || hostLower.endsWith("slack.com");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
