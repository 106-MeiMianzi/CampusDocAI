package com.campusdoc.service.api;

import com.campusdoc.common.ApiResponse;
import com.campusdoc.common.PageResult;
import com.campusdoc.security.SecurityUtils;
import com.campusdoc.service.dto.CardReplacementResponse;
import com.campusdoc.service.dto.CreateCardReplacementRequest;
import com.campusdoc.service.entity.CardReplacementStatus;
import com.campusdoc.service.service.CardReplacementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service/card-replacement")
public class CardReplacementController {

    private final CardReplacementService cardReplacementService;

    public CardReplacementController(CardReplacementService cardReplacementService) {
        this.cardReplacementService = cardReplacementService;
    }

    @PostMapping
    public ApiResponse<CardReplacementResponse> create(@Valid @RequestBody CreateCardReplacementRequest request) {
        return ApiResponse.ok(cardReplacementService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<CardReplacementResponse>> list(
            @RequestParam(required = false) CardReplacementStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(cardReplacementService.list(SecurityUtils.currentUserId(), status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CardReplacementResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(cardReplacementService.detail(SecurityUtils.currentUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<CardReplacementResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(cardReplacementService.cancel(SecurityUtils.currentUserId(), id));
    }
}
