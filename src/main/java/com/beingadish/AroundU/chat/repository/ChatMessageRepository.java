package com.beingadish.AroundU.chat.repository;

import com.beingadish.AroundU.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    long countByConversationIdAndIsReadFalseAndSenderIdNot(Long conversationId, Long userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true " +
           "WHERE m.conversation.id = :conversationId AND m.senderId <> :userId AND m.isRead = false")
    int markAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
