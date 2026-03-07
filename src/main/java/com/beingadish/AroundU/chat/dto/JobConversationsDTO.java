package com.beingadish.AroundU.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Groups conversations under a single job — used by the client/provider view.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobConversationsDTO {

    private Long jobId;
    private String jobTitle;
    private String jobStatus;
    private long totalUnreadCount;
    private String lastMessageContent;
    private String lastMessageAt;
    private boolean archived;
    private List<ConversationResponseDTO> conversations;
}
