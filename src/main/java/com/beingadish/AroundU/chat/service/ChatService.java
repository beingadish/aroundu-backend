package com.beingadish.AroundU.chat.service;

import com.beingadish.AroundU.chat.dto.ChatMessageRequest;
import com.beingadish.AroundU.chat.dto.ChatMessageResponseDTO;
import com.beingadish.AroundU.chat.dto.ConversationResponseDTO;
import com.beingadish.AroundU.chat.dto.JobConversationsDTO;

import java.util.List;

public interface ChatService {

    /**
     * Send a message within a job conversation. Creates conversation if needed.
     * senderRole must be "WORKER" or "CLIENT".
     */
    ChatMessageResponseDTO sendMessage(Long jobId, Long senderId, String senderRole, ChatMessageRequest request);

    /**
     * Get messages for a conversation (paginated, newest first).
     */
    List<ChatMessageResponseDTO> getMessages(Long conversationId, Long userId, int page, int size);

    /**
     * Get all conversations for a user (flat list — suitable for workers).
     */
    List<ConversationResponseDTO> getConversations(Long userId);

    /**
     * Get conversations grouped by job — suitable for clients/providers.
     */
    List<JobConversationsDTO> getConversationsGroupedByJob(Long userId);

    /**
     * Mark all messages in a conversation as delivered for the given user.
     * Returns list of updated message IDs.
     */
    List<Long> markAsDelivered(Long conversationId, Long userId);

    /**
     * Mark all messages in a conversation as read for the given user. Returns
     * list of updated message IDs.
     */
    List<Long> markAsRead(Long conversationId, Long userId);

    /**
     * Archive conversations whose jobs have ended.
     */
    void archiveCompletedConversations();

    /**
     * Delete conversations archived for more than 30 days.
     */
    void deleteExpiredConversations();
}
