package com.beingadish.AroundU.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationResponseDTO {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String jobStatus;
    private Long participantOneId;
    private Long participantTwoId;
    private String participantOneName;
    private String participantTwoName;
    private long unreadCount;
    private String lastMessageContent;
    private Long lastMessageSenderId;
    private String lastMessageSenderRole;
    private boolean archived;
    private LocalDateTime archivedAt;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
