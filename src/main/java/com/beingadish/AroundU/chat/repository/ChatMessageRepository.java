package com.beingadish.AroundU.chat.repository;

import com.beingadish.AroundU.chat.entity.ChatMessage;
import com.beingadish.AroundU.chat.entity.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * Count unread messages (status != READ) sent by the other participant.
     * Uses senderRole rather than senderId because Client and Worker tables
     * have independent ID sequences that can overlap.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation.id = :conversationId "
            + "AND m.status <> :status AND m.senderRole <> :currentRole")
    long countUnreadByRole(@Param("conversationId") Long conversationId,
            @Param("status") MessageStatus status,
            @Param("currentRole") String currentRole);

    /**
     * Find messages not yet delivered, sent by the other participant. Uses
     * senderRole for disambiguation (see countUnreadByRole).
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId "
            + "AND m.senderRole <> :currentRole AND m.status = 'SENT'")
    List<ChatMessage> findUndelivered(@Param("conversationId") Long conversationId,
            @Param("currentRole") String currentRole);

    /**
     * Find messages not yet read, sent by the other participant. Uses
     * senderRole for disambiguation (see countUnreadByRole).
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId "
            + "AND m.senderRole <> :currentRole AND m.status <> 'READ'")
    List<ChatMessage> findUnread(@Param("conversationId") Long conversationId,
            @Param("currentRole") String currentRole);

    /**
     * Bulk mark as delivered. Returns count of updated rows. Uses senderRole
     * for disambiguation (see countUnreadByRole).
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.status = 'DELIVERED' "
            + "WHERE m.conversation.id = :conversationId AND m.senderRole <> :currentRole AND m.status = 'SENT'")
    int markAsDelivered(@Param("conversationId") Long conversationId, @Param("currentRole") String currentRole);

    /**
     * Bulk mark as read. Returns count of updated rows. Uses senderRole for
     * disambiguation (see countUnreadByRole).
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.status = 'READ' "
            + "WHERE m.conversation.id = :conversationId AND m.senderRole <> :currentRole AND m.status <> 'READ'")
    int markAsRead(@Param("conversationId") Long conversationId, @Param("currentRole") String currentRole);

    /**
     * Delete all messages belonging to given conversations.
     */
    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.conversation.id IN :conversationIds")
    int deleteByConversationIds(@Param("conversationIds") List<Long> conversationIds);
}
