package com.socialpublish.auth.security;

import com.socialpublish.auth.exception.OAuth2EmailNotVerifiedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        if (exception instanceof OAuth2EmailNotVerifiedException) {
            getRedirectStrategy().sendRedirect(request, response, "/login?oauth2Error=email_not_verified");
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, "/login?oauth2Error=true");
    }
}

