package com.beingadish.AroundU.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket payload for message delivery / read status updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageStatusUpdate {

    private Long messageId;
    private Long conversationId;
    private String status; // DELIVERED or READ
}
