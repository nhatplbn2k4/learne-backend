package com.learn.learnE_Backend.auth;

import com.learn.learnE_Backend.auth.dto.AuthResponse;
import com.learn.learnE_Backend.auth.dto.LoginRequest;
import com.learn.learnE_Backend.auth.dto.RegisterRequest;
import com.learn.learnE_Backend.auth.dto.RegisterResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được đăng ký");
        }

        // The very first account bootstraps the system: there is no admin yet to approve it.
        boolean isFirstAccount = userRepository.count() == 0;

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(isFirstAccount ? Role.ADMIN : Role.USER)
                .status(isFirstAccount ? UserStatus.ACTIVE : UserStatus.PENDING)
                .build();

        User saved = userRepository.save(user);
        return new RegisterResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getStatus(),
                isFirstAccount
                        ? "Đã tạo tài khoản quản trị đầu tiên, bạn đăng nhập được ngay."
                        : "Đăng ký thành công. Tài khoản cần quản trị viên duyệt trước khi dùng được.");
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (DisabledException ex) {
            // Correct credentials but the account is not usable — say which, rather than a bare 401.
            UserStatus status = userRepository.findByEmail(request.email())
                    .map(User::getStatus)
                    .orElse(UserStatus.PENDING);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, status == UserStatus.REJECTED
                    ? "Tài khoản đã bị từ chối. Liên hệ quản trị viên nếu bạn cho là nhầm."
                    : "Tài khoản đang chờ quản trị viên duyệt.");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String token = jwtService.generateAccessToken(user);
        return AuthResponse.of(token, user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name());
    }
}
