package com.beingadish.AroundU.chat.repository;

import com.beingadish.AroundU.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.job.id = :jobId " +
           "AND ((c.participantOneId = :userA AND c.participantTwoId = :userB) " +
           "OR (c.participantOneId = :userB AND c.participantTwoId = :userA))")
    Optional<Conversation> findByJobAndParticipants(
            @Param("jobId") Long jobId,
            @Param("userA") Long userA,
            @Param("userB") Long userB);

    @Query("SELECT c FROM Conversation c WHERE c.participantOneId = :userId OR c.participantTwoId = :userId " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findByParticipant(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.job.id = :jobId")
    List<Conversation> findByJobId(@Param("jobId") Long jobId);
}
