package com.campusdoc.user.api;

import com.campusdoc.common.ApiResponse;
import com.campusdoc.security.SecurityUtils;
import com.campusdoc.user.dto.LoginRequest;
import com.campusdoc.user.dto.LoginResponse;
import com.campusdoc.user.dto.RegisterRequest;
import com.campusdoc.user.dto.RegisterResponse;
import com.campusdoc.user.service.AuthService;
import com.campusdoc.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout(SecurityUtils.currentUserId());
        return ApiResponse.ok();
    }
}
