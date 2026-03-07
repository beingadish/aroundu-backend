package com.beingadish.AroundU.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageResponseDTO {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderRole;
    private String content;
    private String status; // SENT, DELIVERED, READ
    private LocalDateTime createdAt;
}
