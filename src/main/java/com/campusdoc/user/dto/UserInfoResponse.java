package com.campusdoc.user.dto;

import com.campusdoc.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private Long id;
    private String username;
    private String name;
    private UserRole role;
    private String roleLabel;
    private String avatar;
    private String college;
    private String grade;
    private String major;
    private String className;
    private String phone;
    private String email;
    private String jobTitle;
}
