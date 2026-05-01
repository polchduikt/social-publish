package com.socialpublish.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class HtmxSupport {

    public boolean isHtmxRequest(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    public void redirectTo(HttpServletResponse response, String url) {
        response.setHeader("HX-Redirect", url);
    }
}
