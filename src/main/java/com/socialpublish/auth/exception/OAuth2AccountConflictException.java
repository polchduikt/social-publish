package com.socialpublish.auth.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class OAuth2AccountConflictException extends OAuth2AuthenticationException {

    public OAuth2AccountConflictException(String message) {
        super(new OAuth2Error("account_conflict"), message);
    }
}
