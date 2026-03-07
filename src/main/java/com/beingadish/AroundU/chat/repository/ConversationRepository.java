package com.beingadish.AroundU.chat.repository;

import com.beingadish.AroundU.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.job.id = :jobId "
            + "AND ((c.participantOneId = :userA AND c.participantTwoId = :userB) "
            + "OR (c.participantOneId = :userB AND c.participantTwoId = :userA))")
    Optional<Conversation> findByJobAndParticipants(
            @Param("jobId") Long jobId,
            @Param("userA") Long userA,
            @Param("userB") Long userB);

    @Query("SELECT c FROM Conversation c WHERE c.participantOneId = :userId OR c.participantTwoId = :userId "
            + "ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findByParticipant(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.job.id = :jobId")
    List<Conversation> findByJobId(@Param("jobId") Long jobId);

    /**
     * Conversations archived more than 30 days ago — ready for deletion.
     */
    @Query("SELECT c FROM Conversation c WHERE c.archivedAt IS NOT NULL AND c.archivedAt < :cutoff")
    List<Conversation> findExpiredArchived(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Conversations whose job reached a terminal state but are not yet
     * archived.
     */
    @Query("SELECT c FROM Conversation c WHERE c.archivedAt IS NULL "
            + "AND c.job.jobStatus IN ('COMPLETED', 'CANCELLED')")
    List<Conversation> findConversationsToArchive();

    @Modifying
    @Query("DELETE FROM Conversation c WHERE c.id IN :ids")
    int deleteByIds(@Param("ids") List<Long> ids);
}
