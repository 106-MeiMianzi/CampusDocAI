package com.campusdoc.service.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CardReplacementRequestEntity {

    private Long id;
    private Long userId;
    private CardReplacementType requestType;
    private String contactPhone;
    private String pickupLocation;
    private CardReplacementStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
