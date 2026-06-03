package com.campusdoc.user.api;

import com.campusdoc.common.ApiResponse;
import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.security.SecurityUtils;
import com.campusdoc.user.dto.UpdateAvatarRequest;
import com.campusdoc.user.dto.UserInfoResponse;
import com.campusdoc.user.service.AvatarStorageService;
import com.campusdoc.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final AvatarStorageService avatarStorageService;

    public UserController(UserService userService, AvatarStorageService avatarStorageService) {
        this.userService = userService;
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping("/info")
    public ApiResponse<UserInfoResponse> info() {
        return ApiResponse.ok(userService.getUserInfo(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/avatar")
    public ApiResponse<UserInfoResponse> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        return ApiResponse.ok(userService.updateAvatar(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/avatar/image/{userId}")
    public ResponseEntity<org.springframework.core.io.Resource> avatarImage(@PathVariable Long userId) {
        AvatarStorageService.StoredAvatar avatar = avatarStorageService.load(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .contentType(avatar.mediaType())
                .body(avatar.asResource());
    }
}
