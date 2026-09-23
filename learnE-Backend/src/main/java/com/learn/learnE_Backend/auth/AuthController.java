package com.learn.learnE_Backend.auth;

import com.learn.learnE_Backend.auth.dto.AuthResponse;
import com.learn.learnE_Backend.auth.dto.LoginRequest;
import com.learn.learnE_Backend.auth.dto.RegisterRequest;
import com.learn.learnE_Backend.auth.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final boolean allowRegistration;

    public AuthController(
            AuthService authService,
            @Value("${app.auth.allow-registration:true}") boolean allowRegistration
    ) {
        this.authService = authService;
        this.allowRegistration = allowRegistration;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Closed once the instance is reachable from the internet, so a leaked URL cannot be used
        // to create accounts on someone else's server.
        if (!allowRegistration) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Máy chủ này đã tắt chức năng đăng ký");
        }
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
