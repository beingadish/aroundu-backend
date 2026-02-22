package com.beingadish.AroundU.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationResponseDTO {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long participantOneId;
    private Long participantTwoId;
    private String participantOneName;
    private String participantTwoName;
    private long unreadCount;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
