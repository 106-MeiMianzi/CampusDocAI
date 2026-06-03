package com.campusdoc.user.service;

import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.security.JwtTokenProvider;
import com.campusdoc.user.dto.LoginRequest;
import com.campusdoc.user.dto.LoginResponse;
import com.campusdoc.user.entity.UserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenService authTokenService;

    public AuthService(UserService userService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthTokenService authTokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authTokenService = authTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = userService.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername());
        authTokenService.storeActiveToken(user.getId(), token);
        return new LoginResponse(token, jwtTokenProvider.getExpireSeconds());
    }

    public void logout(Long userId) {
        authTokenService.removeToken(userId);
    }
}
