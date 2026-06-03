package com.campusdoc.service.dto;

import com.campusdoc.service.entity.CardReplacementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCardReplacementRequest {

    @NotNull(message = "申请类型不能为空")
    private CardReplacementType requestType;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    private String pickupLocation;
}
