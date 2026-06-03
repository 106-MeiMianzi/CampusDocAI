CREATE TABLE card_replacement_request (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    request_type     VARCHAR(32)  NOT NULL,
    contact_phone    VARCHAR(32)  NOT NULL,
    pickup_location  VARCHAR(128) NOT NULL DEFAULT '一卡通中心',
    status           VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_card_replacement_user (user_id),
    KEY idx_card_replacement_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
