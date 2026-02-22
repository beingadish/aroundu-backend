package com.beingadish.AroundU.chat.service;

import com.beingadish.AroundU.chat.dto.ChatMessageRequest;
import com.beingadish.AroundU.chat.dto.ChatMessageResponseDTO;
import com.beingadish.AroundU.chat.dto.ConversationResponseDTO;

import java.util.List;

public interface ChatService {

    /**
     * Send a message within a job conversation. Creates conversation if needed.
     */
    ChatMessageResponseDTO sendMessage(Long jobId, Long senderId, ChatMessageRequest request);

    /**
     * Get messages for a conversation (paginated, newest first).
     */
    List<ChatMessageResponseDTO> getMessages(Long conversationId, Long userId, int page, int size);

    /**
     * Get all conversations for a user.
     */
    List<ConversationResponseDTO> getConversations(Long userId);

    /**
     * Mark all messages in a conversation as read for the given user.
     */
    void markAsRead(Long conversationId, Long userId);
}
