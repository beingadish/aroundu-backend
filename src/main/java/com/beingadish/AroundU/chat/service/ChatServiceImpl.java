package com.beingadish.AroundU.chat.service;

import com.beingadish.AroundU.chat.dto.ChatMessageRequest;
import com.beingadish.AroundU.chat.dto.ChatMessageResponseDTO;
import com.beingadish.AroundU.chat.dto.ConversationResponseDTO;
import com.beingadish.AroundU.chat.entity.ChatMessage;
import com.beingadish.AroundU.chat.entity.Conversation;
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
import java.util.List;
import java.util.Optional;
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
    public ChatMessageResponseDTO sendMessage(Long jobId, Long senderId, ChatMessageRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        Long recipientId = request.getRecipientId();

        // Validate sender is a participant in this job
        Long clientId = job.getCreatedBy().getId();
        Long workerId = job.getAssignedTo() != null ? job.getAssignedTo().getId() : null;

        if (workerId == null) {
            throw new ChatValidationException("Cannot send messages for a job without an assigned worker");
        }

        boolean senderIsClient = senderId.equals(clientId);
        boolean senderIsWorker = senderId.equals(workerId);

        if (!senderIsClient && !senderIsWorker) {
            throw new ChatValidationException("Sender is not a participant of this job");
        }

        // Validate recipient matches the other participant
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
                .content(request.getContent().trim())
                .isRead(false)
                .build();
        ChatMessage saved = chatMessageRepository.save(message);

        // Update conversation timestamp
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        log.info("Message sent in conversation {} for job {} by user {}", conversation.getId(), jobId, senderId);
        return chatMessageMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponseDTO> getMessages(Long conversationId, Long userId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));

        // Validate user is a participant
        if (!conversation.getParticipantOneId().equals(userId) &&
            !conversation.getParticipantTwoId().equals(userId)) {
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
    @Transactional
    public void markAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));

        if (!conversation.getParticipantOneId().equals(userId) &&
            !conversation.getParticipantTwoId().equals(userId)) {
            throw new ChatValidationException("User is not a participant of this conversation");
        }

        chatMessageRepository.markAsRead(conversationId, userId);
    }

    private ConversationResponseDTO mapConversationToDto(Conversation conversation, Long currentUserId) {
        ConversationResponseDTO dto = new ConversationResponseDTO();
        dto.setId(conversation.getId());
        dto.setJobId(conversation.getJob().getId());
        dto.setJobTitle(conversation.getJob().getTitle());
        dto.setParticipantOneId(conversation.getParticipantOneId());
        dto.setParticipantTwoId(conversation.getParticipantTwoId());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setCreatedAt(conversation.getCreatedAt());

        // Resolve names
        dto.setParticipantOneName(resolveUserName(conversation.getParticipantOneId()));
        dto.setParticipantTwoName(resolveUserName(conversation.getParticipantTwoId()));

        // Unread count for current user
        long unread = chatMessageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(
                conversation.getId(), currentUserId);
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
