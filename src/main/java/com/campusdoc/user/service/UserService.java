package com.campusdoc.user.service;

import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.user.dto.RegisterRequest;
import com.campusdoc.user.dto.RegisterResponse;
import com.campusdoc.user.dto.UpdateAvatarRequest;
import com.campusdoc.user.dto.UserInfoResponse;
import com.campusdoc.user.entity.UserEntity;
import com.campusdoc.user.entity.UserRole;
import com.campusdoc.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final int MAX_AVATAR_URL_LENGTH = 512;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;

    public UserService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       AvatarStorageService avatarStorageService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorageService = avatarStorageService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        UserEntity entity = new UserEntity();
        entity.setUsername(request.getUsername());
        entity.setPassword(passwordEncoder.encode(request.getPassword()));
        entity.setRole(UserRole.STUDENT);
        userMapper.insert(entity);
        return new RegisterResponse(entity.getId(), entity.getUsername());
    }

    public UserInfoResponse getUserInfo(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return toInfoResponse(user);
    }

    @Transactional
    public UserInfoResponse updateAvatar(Long userId, UpdateAvatarRequest request) {
        String avatarUrl = request.getAvatarUrl().trim();
        if (avatarUrl.startsWith("data:")) {
            avatarUrl = avatarStorageService.saveFromDataUrl(userId, avatarUrl);
        } else if (avatarUrl.length() > MAX_AVATAR_URL_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像地址过长，请上传图片或使用较短的外部链接");
        }
        if (userMapper.updateAvatar(userId, avatarUrl) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return getUserInfo(userId);
    }

    public UserEntity findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public UserEntity requireById(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }

    private UserInfoResponse toInfoResponse(UserEntity user) {
        UserInfoResponse resp = new UserInfoResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
        resp.setRole(user.getRole() != null ? user.getRole() : UserRole.STUDENT);
        resp.setRoleLabel(user.getRole() == UserRole.TEACHER ? "教师" : "学生");
        resp.setAvatar(user.getAvatarUrl());
        resp.setCollege(user.getCollege());
        resp.setGrade(user.getGrade());
        resp.setMajor(user.getMajor());
        resp.setClassName(user.getClassName());
        resp.setPhone(user.getPhone());
        resp.setEmail(user.getEmail());
        resp.setJobTitle(user.getJobTitle());
        return resp;
    }
}
