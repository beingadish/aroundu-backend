package com.beingadish.AroundU.chat.service;

import com.beingadish.AroundU.chat.dto.ChatMessageRequest;
import com.beingadish.AroundU.chat.dto.ChatMessageResponseDTO;
import com.beingadish.AroundU.chat.dto.ConversationResponseDTO;
import com.beingadish.AroundU.chat.dto.JobConversationsDTO;
import com.beingadish.AroundU.chat.entity.ChatMessage;
import com.beingadish.AroundU.chat.entity.Conversation;
import com.beingadish.AroundU.chat.entity.MessageStatus;
import com.beingadish.AroundU.chat.exception.ChatValidationException;
import com.beingadish.AroundU.chat.exception.ConversationNotFoundException;
import com.beingadish.AroundU.chat.mapper.ChatMessageMapper;
import com.beingadish.AroundU.chat.repository.ChatMessageRepository;
import com.beingadish.AroundU.chat.repository.ConversationRepository;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.exception.JobNotFoundException;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.repository.ClientReadRepository;
import com.beingadish.AroundU.user.repository.WorkerReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final JobRepository jobRepository;
    private final ClientReadRepository clientReadRepository;
    private final WorkerReadRepository workerReadRepository;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    @Transactional
    public ChatMessageResponseDTO sendMessage(Long jobId, Long senderId, String senderRole, ChatMessageRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        Long recipientId = request.getRecipientId();

        Long clientId = job.getCreatedBy().getId();
        Long workerId = job.getAssignedTo() != null ? job.getAssignedTo().getId() : null;

        if (workerId == null) {
            throw new ChatValidationException("Cannot send messages for a job without an assigned worker");
        }

        boolean senderIsClient = "CLIENT".equalsIgnoreCase(senderRole) && senderId.equals(clientId);
        boolean senderIsWorker = "WORKER".equalsIgnoreCase(senderRole) && senderId.equals(workerId);

        if (!senderIsClient && !senderIsWorker) {
            throw new ChatValidationException("Sender is not a participant of this job");
        }

        if (senderIsClient && !recipientId.equals(workerId)) {
            throw new ChatValidationException("Recipient must be the assigned worker");
        }
        if (senderIsWorker && !recipientId.equals(clientId)) {
            throw new ChatValidationException("Recipient must be the job owner");
        }

        // Find or create conversation
        Conversation conversation = conversationRepository
                .findByJobAndParticipants(jobId, clientId, workerId)
                .orElseGet(() -> {
                    Conversation newConvo = Conversation.builder()
                            .job(job)
                            .participantOneId(clientId)
                            .participantTwoId(workerId)
                            .build();
                    return conversationRepository.save(newConvo);
                });

        // Create and save message
        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .senderId(senderId)
                .senderRole(senderRole.toUpperCase())
                .content(request.getContent().trim())
                .status(MessageStatus.SENT)
                .build();
        ChatMessage saved = chatMessageRepository.save(message);

        // Update conversation metadata
        String preview = request.getContent().trim();
        if (preview.length() > 200) {
            preview = preview.substring(0, 200);
        }
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastMessageContent(preview);
        conversation.setLastMessageSenderId(senderId);
        conversationRepository.save(conversation);

        log.info("Message sent in conversation {} for job {} by {} {}", conversation.getId(), jobId, senderRole, senderId);
        return chatMessageMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponseDTO> getMessages(Long conversationId, Long userId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));

        if (!conversation.getParticipantOneId().equals(userId)
                && !conversation.getParticipantTwoId().equals(userId)) {
            throw new ChatValidationException("User is not a participant of this conversation");
        }

        Page<ChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(page, size));

        return chatMessageMapper.toDtoList(messages.getContent());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponseDTO> getConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByParticipant(userId);

        return conversations.stream()
                .map(c -> mapConversationToDto(c, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobConversationsDTO> getConversationsGroupedByJob(Long userId) {
        List<Conversation> conversations = conversationRepository.findByParticipant(userId);

        // Group by jobId
        Map<Long, List<Conversation>> byJob = conversations.stream()
                .collect(Collectors.groupingBy(c -> c.getJob().getId(), LinkedHashMap::new, Collectors.toList()));

        List<JobConversationsDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<Conversation>> entry : byJob.entrySet()) {
            List<Conversation> jobConversations = entry.getValue();
            Conversation first = jobConversations.get(0);
            Job job = first.getJob();

            List<ConversationResponseDTO> convDtos = jobConversations.stream()
                    .map(c -> mapConversationToDto(c, userId))
                    .collect(Collectors.toList());

            long totalUnread = convDtos.stream().mapToLong(ConversationResponseDTO::getUnreadCount).sum();

            // Find the most recent message across all conversations for this job
            LocalDateTime latestMsg = jobConversations.stream()
                    .map(Conversation::getLastMessageAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            String latestContent = jobConversations.stream()
                    .filter(c -> c.getLastMessageAt() != null)
                    .max(Comparator.comparing(Conversation::getLastMessageAt))
                    .map(Conversation::getLastMessageContent)
                    .orElse(null);

            boolean archived = first.getArchivedAt() != null;

            result.add(JobConversationsDTO.builder()
                    .jobId(job.getId())
                    .jobTitle(job.getTitle())
                    .jobStatus(job.getJobStatus().name())
                    .totalUnreadCount(totalUnread)
                    .lastMessageContent(latestContent)
                    .lastMessageAt(latestMsg != null ? latestMsg.toString() : null)
                    .archived(archived)
                    .conversations(convDtos)
                    .build());
        }

        // Sort by most recent message
        result.sort((a, b) -> {
            if (a.getLastMessageAt() == null && b.getLastMessageAt() == null) {
                return 0;
            }
            if (a.getLastMessageAt() == null) {
                return 1;
            }
            if (b.getLastMessageAt() == null) {
                return -1;
            }
            return b.getLastMessageAt().compareTo(a.getLastMessageAt());
        });

        return result;
    }

    @Override
    @Transactional
    public List<Long> markAsDelivered(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));

        validateParticipant(conversation, userId);

        List<ChatMessage> undelivered = chatMessageRepository.findUndelivered(conversationId, userId);
        List<Long> ids = undelivered.stream().map(ChatMessage::getId).collect(Collectors.toList());

        if (!ids.isEmpty()) {
            chatMessageRepository.markAsDelivered(conversationId, userId);
        }

        return ids;
    }

    @Override
    @Transactional
    public List<Long> markAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));

        validateParticipant(conversation, userId);

        List<ChatMessage> unread = chatMessageRepository.findUnread(conversationId, userId);
        List<Long> ids = unread.stream().map(ChatMessage::getId).collect(Collectors.toList());

        if (!ids.isEmpty()) {
            chatMessageRepository.markAsRead(conversationId, userId);
        }

        return ids;
    }

    @Override
    @Transactional
    public void archiveCompletedConversations() {
        List<Conversation> toArchive = conversationRepository.findConversationsToArchive();
        LocalDateTime now = LocalDateTime.now();
        for (Conversation c : toArchive) {
            c.setArchivedAt(now);
        }
        if (!toArchive.isEmpty()) {
            conversationRepository.saveAll(toArchive);
            log.info("Archived {} conversations for completed/cancelled jobs", toArchive.size());
        }
    }

    @Override
    @Transactional
    public void deleteExpiredConversations() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<Conversation> expired = conversationRepository.findExpiredArchived(cutoff);

        if (!expired.isEmpty()) {
            List<Long> ids = expired.stream().map(Conversation::getId).collect(Collectors.toList());
            chatMessageRepository.deleteByConversationIds(ids);
            conversationRepository.deleteByIds(ids);
            log.info("Deleted {} expired conversations (archived > 30 days)", ids.size());
        }
    }

    // ── Private helpers ──────────────────────────────────────────
    private void validateParticipant(Conversation conversation, Long userId) {
        if (!conversation.getParticipantOneId().equals(userId)
                && !conversation.getParticipantTwoId().equals(userId)) {
            throw new ChatValidationException("User is not a participant of this conversation");
        }
    }

    private ConversationResponseDTO mapConversationToDto(Conversation conversation, Long currentUserId) {
        ConversationResponseDTO dto = new ConversationResponseDTO();
        dto.setId(conversation.getId());
        dto.setJobId(conversation.getJob().getId());
        dto.setJobTitle(conversation.getJob().getTitle());
        dto.setJobStatus(conversation.getJob().getJobStatus().name());
        dto.setParticipantOneId(conversation.getParticipantOneId());
        dto.setParticipantTwoId(conversation.getParticipantTwoId());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setLastMessageContent(conversation.getLastMessageContent());
        dto.setLastMessageSenderId(conversation.getLastMessageSenderId());
        dto.setCreatedAt(conversation.getCreatedAt());

        // Archive status
        dto.setArchivedAt(conversation.getArchivedAt());
        dto.setArchived(conversation.getArchivedAt() != null);

        // Resolve names
        dto.setParticipantOneName(resolveUserName(conversation.getParticipantOneId()));
        dto.setParticipantTwoName(resolveUserName(conversation.getParticipantTwoId()));

        // Unread count: messages not READ, sent by the other participant
        long unread = chatMessageRepository.countByConversationIdAndStatusNotAndSenderIdNot(
                conversation.getId(), MessageStatus.READ, currentUserId);
        dto.setUnreadCount(unread);

        return dto;
    }

    private String resolveUserName(Long userId) {
        Optional<Client> client = clientReadRepository.findById(userId);
        if (client.isPresent()) {
            return client.get().getName();
        }
        Optional<Worker> worker = workerReadRepository.findById(userId);
        return worker.map(Worker::getName).orElse("Unknown");
    }
}
