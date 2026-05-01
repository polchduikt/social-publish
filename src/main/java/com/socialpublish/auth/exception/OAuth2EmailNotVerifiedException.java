package com.socialpublish.auth.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class OAuth2EmailNotVerifiedException extends OAuth2AuthenticationException {

    public OAuth2EmailNotVerifiedException(String message) {
        super(new OAuth2Error("email_not_verified"), message);
    }
}
