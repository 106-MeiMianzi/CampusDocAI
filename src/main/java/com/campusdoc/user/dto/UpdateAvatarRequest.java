package com.campusdoc.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAvatarRequest {

    @NotBlank(message = "头像地址不能为空")
    private String avatarUrl;
}
