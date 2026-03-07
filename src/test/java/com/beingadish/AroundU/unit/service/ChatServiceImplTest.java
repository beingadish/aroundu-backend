package com.beingadish.AroundU.unit.service;

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
import com.beingadish.AroundU.chat.service.ChatServiceImpl;
import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.fixtures.TestFixtures;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.exception.JobNotFoundException;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.repository.ClientReadRepository;
import com.beingadish.AroundU.user.repository.WorkerReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatServiceImpl")
class ChatServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private ClientReadRepository clientReadRepository;
    @Mock
    private WorkerReadRepository workerReadRepository;
    @Mock
    private ChatMessageMapper chatMessageMapper;

    @InjectMocks
    private ChatServiceImpl chatService;

    private Client client;
    private Worker worker;
    private Job assignedJob;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        client = TestFixtures.client(1L);
        worker = TestFixtures.worker(10L);
        assignedJob = TestFixtures.assignedJob(100L, client, worker);

        conversation = Conversation.builder()
                .id(500L)
                .job(assignedJob)
                .participantOneId(client.getId())
                .participantTwoId(worker.getId())
                .build();
    }

    // ── sendMessage ──────────────────────────────────────────────
    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("success – client sends message to worker")
        void clientSendsMessage() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setRecipientId(worker.getId());
            request.setContent("Hello worker!");

            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(conversationRepository.findByJobAndParticipants(100L, 1L, 10L))
                    .thenReturn(Optional.of(conversation));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> {
                        ChatMessage m = inv.getArgument(0);
                        m.setId(1000L);
                        return m;
                    });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            ChatMessageResponseDTO expected = new ChatMessageResponseDTO();
            expected.setId(1000L);
            expected.setConversationId(500L);
            expected.setSenderId(1L);
            expected.setContent("Hello worker!");
            expected.setStatus("SENT");
            when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(expected);

            ChatMessageResponseDTO result = chatService.sendMessage(100L, 1L, "CLIENT", request);

            assertNotNull(result);
            assertEquals(1000L, result.getId());
            assertEquals("Hello worker!", result.getContent());
            assertEquals("SENT", result.getStatus());

            ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
            verify(chatMessageRepository).save(captor.capture());
            assertEquals(MessageStatus.SENT, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("success – worker sends message to client")
        void workerSendsMessage() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setRecipientId(client.getId());
            request.setContent("On my way!");

            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(conversationRepository.findByJobAndParticipants(100L, 1L, 10L))
                    .thenReturn(Optional.of(conversation));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> {
                        ChatMessage m = inv.getArgument(0);
                        m.setId(1001L);
                        return m;
                    });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

            ChatMessageResponseDTO expected = new ChatMessageResponseDTO();
            expected.setId(1001L);
            when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(expected);

            ChatMessageResponseDTO result = chatService.sendMessage(100L, 10L, "WORKER", request);

            assertNotNull(result);
            assertEquals(1001L, result.getId());
        }

        @Test
        @DisplayName("creates conversation if not exists")
        void createsConversationIfNotExists() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setRecipientId(worker.getId());
            request.setContent("First message");

            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(conversationRepository.findByJobAndParticipants(100L, 1L, 10L))
                    .thenReturn(Optional.empty());
            when(conversationRepository.save(any(Conversation.class)))
                    .thenAnswer(inv -> {
                        Conversation c = inv.getArgument(0);
                        c.setId(501L);
                        return c;
                    });
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> {
                        ChatMessage m = inv.getArgument(0);
                        m.setId(1002L);
                        return m;
                    });

            ChatMessageResponseDTO expected = new ChatMessageResponseDTO();
            expected.setId(1002L);
            when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(expected);

            chatService.sendMessage(100L, 1L, "CLIENT", request);

            // Saved conversation twice: once for create, once for metadata update
            verify(conversationRepository, times(2)).save(any(Conversation.class));
        }

        @Test
        @DisplayName("throws if job not found")
        void throwsJobNotFound() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setRecipientId(worker.getId());
            request.setContent("Hi");

            when(jobRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(JobNotFoundException.class,
                    () -> chatService.sendMessage(999L, 1L, "CLIENT", request));
        }

        @Test
        @DisplayName("throws if job has no assigned worker")
        void throwsNoAssignedWorker() {
            Job unassigned = TestFixtures.job(101L, client);
            ChatMessageRequest request = new ChatMessageRequest();
            request.setRecipientId(10L);
            request.setContent("Hi");

            when(jobRepository.findById(101L)).thenReturn(Optional.of(unassigned));

            assertThrows(ChatValidationException.class,
                    () -> chatService.sendMessage(101L, 1L, "CLIENT", request));
        }

        @Test
        @DisplayName("throws if sender is not a participant")
        void throwsSenderNotParticipant() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setRecipientId(worker.getId());
            request.setContent("Hi");

            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));

            assertThrows(ChatValidationException.class,
                    () -> chatService.sendMessage(100L, 999L, "CLIENT", request));
        }

        @Test
        @DisplayName("throws if client sends to wrong recipient")
        void throwsWrongRecipient() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setRecipientId(999L); // not the assigned worker
            request.setContent("Hi");

            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));

            assertThrows(ChatValidationException.class,
                    () -> chatService.sendMessage(100L, 1L, "CLIENT", request));
        }

        @Test
        @DisplayName("truncates last message preview to 200 chars")
        void truncatesPreview() {
            String longContent = "A".repeat(300);
            ChatMessageRequest request = new ChatMessageRequest();
            request.setRecipientId(worker.getId());
            request.setContent(longContent);

            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(conversationRepository.findByJobAndParticipants(100L, 1L, 10L))
                    .thenReturn(Optional.of(conversation));
            when(chatMessageRepository.save(any(ChatMessage.class)))
                    .thenAnswer(inv -> {
                        ChatMessage m = inv.getArgument(0);
                        m.setId(1003L);
                        return m;
                    });
            when(conversationRepository.save(any(Conversation.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(chatMessageMapper.toDto(any(ChatMessage.class))).thenReturn(new ChatMessageResponseDTO());

            chatService.sendMessage(100L, 1L, "CLIENT", request);

            ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
            verify(conversationRepository, atLeastOnce()).save(captor.capture());
            Conversation saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertEquals(200, saved.getLastMessageContent().length());
        }
    }

    // ── getMessages ──────────────────────────────────────────────
    @Nested
    @DisplayName("getMessages")
    class GetMessages {

        @Test
        @DisplayName("success – returns paginated messages")
        void returnsPaginatedMessages() {
            ChatMessage msg = ChatMessage.builder()
                    .id(1L).conversation(conversation).senderId(1L)
                    .senderRole("CLIENT").content("Hi").status(MessageStatus.SENT).build();
            Page<ChatMessage> page = new PageImpl<>(List.of(msg));

            when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversation));
            when(chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(eq(500L), any(PageRequest.class)))
                    .thenReturn(page);

            ChatMessageResponseDTO dto = new ChatMessageResponseDTO();
            dto.setId(1L);
            when(chatMessageMapper.toDtoList(anyList())).thenReturn(List.of(dto));

            List<ChatMessageResponseDTO> result = chatService.getMessages(500L, 1L, 0, 50);

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
        }

        @Test
        @DisplayName("throws if conversation not found")
        void throwsConversationNotFound() {
            when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ConversationNotFoundException.class,
                    () -> chatService.getMessages(999L, 1L, 0, 50));
        }

        @Test
        @DisplayName("throws if user is not a participant")
        void throwsNotParticipant() {
            when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversation));

            assertThrows(ChatValidationException.class,
                    () -> chatService.getMessages(500L, 999L, 0, 50));
        }
    }

    // ── getConversations ─────────────────────────────────────────
    @Nested
    @DisplayName("getConversations")
    class GetConversations {

        @Test
        @DisplayName("success – returns flat conversation list")
        void returnsFlatList() {
            conversation.setLastMessageAt(LocalDateTime.now());
            conversation.setLastMessageContent("Latest");
            conversation.setLastMessageSenderId(1L);

            when(conversationRepository.findByParticipant(1L)).thenReturn(List.of(conversation));
            when(clientReadRepository.findById(1L)).thenReturn(Optional.of(client));
            when(workerReadRepository.findById(10L)).thenReturn(Optional.of(worker));
            when(chatMessageRepository.countByConversationIdAndStatusNotAndSenderIdNot(
                    eq(500L), eq(MessageStatus.READ), eq(1L))).thenReturn(3L);

            List<ConversationResponseDTO> result = chatService.getConversations(1L);

            assertEquals(1, result.size());
            ConversationResponseDTO dto = result.get(0);
            assertEquals(500L, dto.getId());
            assertEquals(100L, dto.getJobId());
            assertEquals("Latest", dto.getLastMessageContent());
            assertEquals(3L, dto.getUnreadCount());
            assertFalse(dto.isArchived());
        }

        @Test
        @DisplayName("marks archived conversations correctly")
        void marksArchivedCorrectly() {
            conversation.setArchivedAt(LocalDateTime.now().minusDays(5));

            when(conversationRepository.findByParticipant(1L)).thenReturn(List.of(conversation));
            when(clientReadRepository.findById(1L)).thenReturn(Optional.of(client));
            when(workerReadRepository.findById(10L)).thenReturn(Optional.of(worker));
            when(chatMessageRepository.countByConversationIdAndStatusNotAndSenderIdNot(anyLong(), any(), anyLong()))
                    .thenReturn(0L);

            List<ConversationResponseDTO> result = chatService.getConversations(1L);

            assertTrue(result.get(0).isArchived());
            assertNotNull(result.get(0).getArchivedAt());
        }
    }

    // ── getConversationsGroupedByJob ─────────────────────────────
    @Nested
    @DisplayName("getConversationsGroupedByJob")
    class GetConversationsGroupedByJob {

        @Test
        @DisplayName("success – groups conversations by job")
        void groupsByJob() {
            Worker worker2 = TestFixtures.worker(20L);
            Conversation conv2 = Conversation.builder()
                    .id(501L).job(assignedJob)
                    .participantOneId(1L).participantTwoId(20L)
                    .lastMessageAt(LocalDateTime.now().minusHours(1))
                    .lastMessageContent("From worker 2").build();
            conversation.setLastMessageAt(LocalDateTime.now());
            conversation.setLastMessageContent("From worker 1");

            when(conversationRepository.findByParticipant(1L)).thenReturn(List.of(conversation, conv2));
            when(clientReadRepository.findById(1L)).thenReturn(Optional.of(client));
            when(workerReadRepository.findById(10L)).thenReturn(Optional.of(worker));
            when(workerReadRepository.findById(20L)).thenReturn(Optional.of(worker2));
            when(chatMessageRepository.countByConversationIdAndStatusNotAndSenderIdNot(anyLong(), any(), anyLong()))
                    .thenReturn(2L);

            List<JobConversationsDTO> result = chatService.getConversationsGroupedByJob(1L);

            assertEquals(1, result.size());
            JobConversationsDTO group = result.get(0);
            assertEquals(100L, group.getJobId());
            assertEquals(2, group.getConversations().size());
            assertEquals(4L, group.getTotalUnreadCount()); // 2 per conversation
        }
    }

    // ── markAsDelivered ──────────────────────────────────────────
    @Nested
    @DisplayName("markAsDelivered")
    class MarkAsDelivered {

        @Test
        @DisplayName("success – marks undelivered messages")
        void marksUndelivered() {
            ChatMessage msg1 = ChatMessage.builder().id(1L).conversation(conversation)
                    .senderId(1L).senderRole("CLIENT").content("Hi").status(MessageStatus.SENT).build();
            ChatMessage msg2 = ChatMessage.builder().id(2L).conversation(conversation)
                    .senderId(1L).senderRole("CLIENT").content("Hello").status(MessageStatus.SENT).build();

            when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversation));
            when(chatMessageRepository.findUndelivered(500L, 10L)).thenReturn(List.of(msg1, msg2));
            when(chatMessageRepository.markAsDelivered(500L, 10L)).thenReturn(2);

            List<Long> ids = chatService.markAsDelivered(500L, 10L);

            assertEquals(List.of(1L, 2L), ids);
            verify(chatMessageRepository).markAsDelivered(500L, 10L);
        }

        @Test
        @DisplayName("returns empty list when nothing to deliver")
        void returnsEmptyWhenNothingToDeliver() {
            when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversation));
            when(chatMessageRepository.findUndelivered(500L, 10L)).thenReturn(List.of());

            List<Long> ids = chatService.markAsDelivered(500L, 10L);

            assertTrue(ids.isEmpty());
            verify(chatMessageRepository, never()).markAsDelivered(anyLong(), anyLong());
        }
    }

    // ── markAsRead ───────────────────────────────────────────────
    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("success – marks unread messages")
        void marksUnread() {
            ChatMessage msg = ChatMessage.builder().id(3L).conversation(conversation)
                    .senderId(1L).senderRole("CLIENT").content("Read me").status(MessageStatus.DELIVERED).build();

            when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversation));
            when(chatMessageRepository.findUnread(500L, 10L)).thenReturn(List.of(msg));
            when(chatMessageRepository.markAsRead(500L, 10L)).thenReturn(1);

            List<Long> ids = chatService.markAsRead(500L, 10L);

            assertEquals(List.of(3L), ids);
            verify(chatMessageRepository).markAsRead(500L, 10L);
        }

        @Test
        @DisplayName("throws if conversation not found")
        void throwsConversationNotFound() {
            when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ConversationNotFoundException.class,
                    () -> chatService.markAsRead(999L, 10L));
        }

        @Test
        @DisplayName("throws if user not participant")
        void throwsNotParticipant() {
            when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversation));

            assertThrows(ChatValidationException.class,
                    () -> chatService.markAsRead(500L, 999L));
        }
    }

    // ── archiveCompletedConversations ────────────────────────────
    @Nested
    @DisplayName("archiveCompletedConversations")
    class ArchiveConversations {

        @Test
        @DisplayName("archives conversations for completed jobs")
        void archivesCompleted() {
            Conversation c1 = Conversation.builder().id(600L).job(assignedJob)
                    .participantOneId(1L).participantTwoId(10L).build();
            Conversation c2 = Conversation.builder().id(601L).job(assignedJob)
                    .participantOneId(1L).participantTwoId(10L).build();

            when(conversationRepository.findConversationsToArchive()).thenReturn(List.of(c1, c2));

            chatService.archiveCompletedConversations();

            assertNotNull(c1.getArchivedAt());
            assertNotNull(c2.getArchivedAt());
            verify(conversationRepository).saveAll(List.of(c1, c2));
        }

        @Test
        @DisplayName("does nothing when no conversations to archive")
        void doesNothingWhenEmpty() {
            when(conversationRepository.findConversationsToArchive()).thenReturn(List.of());

            chatService.archiveCompletedConversations();

            verify(conversationRepository, never()).saveAll(anyList());
        }
    }

    // ── deleteExpiredConversations ────────────────────────────────
    @Nested
    @DisplayName("deleteExpiredConversations")
    class DeleteExpired {

        @Test
        @DisplayName("deletes messages and conversations archived > 30 days")
        void deletesExpired() {
            Conversation expired = Conversation.builder().id(700L).job(assignedJob)
                    .participantOneId(1L).participantTwoId(10L)
                    .archivedAt(LocalDateTime.now().minusDays(35)).build();

            when(conversationRepository.findExpiredArchived(any(LocalDateTime.class)))
                    .thenReturn(List.of(expired));
            when(chatMessageRepository.deleteByConversationIds(List.of(700L))).thenReturn(5);
            when(conversationRepository.deleteByIds(List.of(700L))).thenReturn(1);

            chatService.deleteExpiredConversations();

            verify(chatMessageRepository).deleteByConversationIds(List.of(700L));
            verify(conversationRepository).deleteByIds(List.of(700L));
        }

        @Test
        @DisplayName("does nothing when no expired conversations")
        void doesNothingWhenEmpty() {
            when(conversationRepository.findExpiredArchived(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            chatService.deleteExpiredConversations();

            verify(chatMessageRepository, never()).deleteByConversationIds(anyList());
            verify(conversationRepository, never()).deleteByIds(anyList());
        }
    }
}
