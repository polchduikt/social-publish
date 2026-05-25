package com.socialpublish.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String LINK_REDIRECT_FLAG = "LINK_GOOGLE_REDIRECT";

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        clearAuthenticationAttributes(request);

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object linkFlag = session.getAttribute(LINK_REDIRECT_FLAG);
            if (linkFlag != null) {
                session.removeAttribute(LINK_REDIRECT_FLAG);
                getRedirectStrategy().sendRedirect(request, response,
                        "/settings?message=Google+account+linked+successfully");
                return;
            }
        }

        getRedirectStrategy().sendRedirect(request, response, "/dashboard");
    }
}
