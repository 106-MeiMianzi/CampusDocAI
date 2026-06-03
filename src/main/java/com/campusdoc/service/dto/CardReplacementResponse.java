package com.campusdoc.service.dto;

import com.campusdoc.service.entity.CardReplacementStatus;
import com.campusdoc.service.entity.CardReplacementType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardReplacementResponse {

    private Long id;
    private CardReplacementType requestType;
    private String contactPhone;
    private String pickupLocation;
    private CardReplacementStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
