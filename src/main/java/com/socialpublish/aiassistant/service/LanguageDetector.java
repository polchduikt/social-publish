package com.socialpublish.aiassistant.service;

import org.springframework.stereotype.Component;

@Component
public class LanguageDetector {

    public boolean looksUkrainian(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.UnicodeBlock.of(value.charAt(i)) == Character.UnicodeBlock.CYRILLIC) {
                return true;
            }
        }
        return false;
    }
}
