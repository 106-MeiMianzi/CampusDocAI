package com.campusdoc.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserEntity {

    private Long id;
    private String username;
    private String password;
    private UserRole role;
    private String displayName;
    private String avatarUrl;
    private String college;
    private String grade;
    private String major;
    private String className;
    private String phone;
    private String email;
    private String jobTitle;
    private LocalDateTime createdAt;
}
