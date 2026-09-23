package com.learn.learnE_Backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.learnE_Backend.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // Own instance rather than an injected bean: Spring Boot 4 autoconfigures a
    // Jackson 3 (tools.jackson.databind) ObjectMapper, not this classic Jackson 2 type.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiError error = new ApiError(
                Instant.now().toString(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Cần đăng nhập (hoặc thông tin đăng nhập không đúng) để truy cập tài nguyên này.",
                request.getRequestURI()
        );
        objectMapper.writeValue(response.getWriter(), error);
    }
}
