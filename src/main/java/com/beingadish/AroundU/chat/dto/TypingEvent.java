package com.beingadish.AroundU.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket payload for typing indicator events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingEvent {

    private Long conversationId;
    private Long userId;
    private boolean typing;
}
