package com.campusdoc.service.service;

import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import com.campusdoc.common.PageResult;
import com.campusdoc.service.dto.CardReplacementResponse;
import com.campusdoc.service.dto.CreateCardReplacementRequest;
import com.campusdoc.service.entity.CardReplacementRequestEntity;
import com.campusdoc.service.entity.CardReplacementStatus;
import com.campusdoc.service.mapper.CardReplacementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CardReplacementService {

    private static final String DEFAULT_PICKUP_LOCATION = "一卡通中心";

    private final CardReplacementMapper cardReplacementMapper;

    public CardReplacementService(CardReplacementMapper cardReplacementMapper) {
        this.cardReplacementMapper = cardReplacementMapper;
    }

    @Transactional
    public CardReplacementResponse create(Long userId, CreateCardReplacementRequest request) {
        CardReplacementRequestEntity entity = new CardReplacementRequestEntity();
        entity.setUserId(userId);
        entity.setRequestType(request.getRequestType());
        entity.setContactPhone(request.getContactPhone().trim());
        entity.setPickupLocation(resolvePickupLocation(request.getPickupLocation()));
        entity.setStatus(CardReplacementStatus.SUBMITTED);
        cardReplacementMapper.insert(entity);
        return toResponse(entity);
    }

    public PageResult<CardReplacementResponse> list(Long userId, CardReplacementStatus status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<CardReplacementResponse> list = cardReplacementMapper.listByUser(userId, status, offset, safeSize)
                .stream()
                .map(this::toResponse)
                .toList();
        long total = cardReplacementMapper.countByUser(userId, status);
        return new PageResult<>(list, total, safePage, safeSize);
    }

    public CardReplacementResponse detail(Long userId, Long id) {
        return toResponse(requireOwned(userId, id));
    }

    @Transactional
    public CardReplacementResponse cancel(Long userId, Long id) {
        CardReplacementRequestEntity entity = requireOwned(userId, id);
        if (entity.getStatus() != CardReplacementStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待受理状态的申请可取消");
        }
        cardReplacementMapper.updateStatus(id, userId, CardReplacementStatus.CANCELLED);
        entity.setStatus(CardReplacementStatus.CANCELLED);
        return toResponse(entity);
    }

    public CardReplacementRequestEntity requireOwned(Long userId, Long id) {
        CardReplacementRequestEntity entity = cardReplacementMapper.findByIdAndUserId(id, userId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CARD_REPLACEMENT_NOT_FOUND);
        }
        return entity;
    }

    private String resolvePickupLocation(String pickupLocation) {
        if (pickupLocation == null || pickupLocation.isBlank()) {
            return DEFAULT_PICKUP_LOCATION;
        }
        return pickupLocation.trim();
    }

    private CardReplacementResponse toResponse(CardReplacementRequestEntity entity) {
        return new CardReplacementResponse(
                entity.getId(),
                entity.getRequestType(),
                entity.getContactPhone(),
                entity.getPickupLocation(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
