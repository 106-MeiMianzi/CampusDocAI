package com.campusdoc.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "ok"),
    BAD_REQUEST(40001, "请求参数错误"),
    UNAUTHORIZED(40101, "未登录或 token 无效"),
    FORBIDDEN(40301, "无权限"),
    NOT_FOUND(40401, "资源不存在"),
    CONFLICT(40901, "资源冲突"),
    INTERNAL_ERROR(50001, "服务器内部错误"),
    USERNAME_EXISTS(40002, "用户名已存在"),
    INVALID_CREDENTIALS(40102, "用户名或密码错误"),
    DOCUMENT_NOT_FOUND(40402, "文档不存在"),
    CONVERSATION_NOT_FOUND(40403, "会话不存在"),
    CARD_REPLACEMENT_NOT_FOUND(40404, "校园卡补办申请不存在"),
    FILE_TOO_LARGE(40003, "文件超过大小限制"),
    TOO_MANY_FILES(40004, "单次上传文件数量超限"),
    UNSUPPORTED_FILE_TYPE(40005, "不支持的文件类型"),
    AI_CALL_FAILED(50002, "AI 调用失败"),
    PARSE_FAILED(50003, "文档解析失败");

    private final int code;
    private final String message;
}
