package com.beingadish.AroundU.chat.controller;

import com.beingadish.AroundU.chat.dto.ChatMessageRequest;
import com.beingadish.AroundU.chat.dto.ChatMessageResponseDTO;
import com.beingadish.AroundU.chat.dto.MessageStatusUpdate;
import com.beingadish.AroundU.chat.dto.TypingEvent;
import com.beingadish.AroundU.chat.service.ChatService;
import com.beingadish.AroundU.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

/**
 * STOMP WebSocket controller for real-time chat operations.
 *
 * <p>
 * Subscription destinations:
 * <ul>
 * <li>/topic/conversation/{conversationId} — new messages and status
 * updates</li>
 * <li>/topic/conversation/{conversationId}/typing — typing indicators</li>
 * </ul>
 *
 * <p>
 * Send destinations (prefixed with /app):
 * <ul>
 * <li>/app/chat.send/{jobId} — send a message</li>
 * <li>/app/chat.typing/{conversationId} — typing indicator</li>
 * <li>/app/chat.delivered/{conversationId} — mark messages as delivered</li>
 * <li>/app/chat.read/{conversationId} — mark messages as read</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send/{jobId}")
    public void sendMessage(@DestinationVariable Long jobId,
            @Payload ChatMessageRequest request,
            Principal principal) {
        UserPrincipal user = (UserPrincipal) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        String role = user.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("UNKNOWN");

        ChatMessageResponseDTO dto = chatService.sendMessage(jobId, user.getId(), role, request);

        // Broadcast to all subscribers of this conversation
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + dto.getConversationId(), dto);
    }

    @MessageMapping("/chat.typing/{conversationId}")
    public void typingIndicator(@DestinationVariable Long conversationId,
            @Payload TypingEvent event,
            Principal principal) {
        UserPrincipal user = (UserPrincipal) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        event.setConversationId(conversationId);
        event.setUserId(user.getId());

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId + "/typing", event);
    }

    @MessageMapping("/chat.delivered/{conversationId}")
    public void markDelivered(@DestinationVariable Long conversationId,
            Principal principal) {
        UserPrincipal user = (UserPrincipal) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        String role = user.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("UNKNOWN");

        List<Long> updatedIds = chatService.markAsDelivered(conversationId, user.getId(), role);

        for (Long msgId : updatedIds) {
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + conversationId,
                    MessageStatusUpdate.builder()
                            .messageId(msgId)
                            .conversationId(conversationId)
                            .status("DELIVERED")
                            .build());
        }
    }

    @MessageMapping("/chat.read/{conversationId}")
    public void markRead(@DestinationVariable Long conversationId,
            Principal principal) {
        UserPrincipal user = (UserPrincipal) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        String role = user.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("UNKNOWN");

        List<Long> updatedIds = chatService.markAsRead(conversationId, user.getId(), role);

        for (Long msgId : updatedIds) {
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + conversationId,
                    MessageStatusUpdate.builder()
                            .messageId(msgId)
                            .conversationId(conversationId)
                            .status("READ")
                            .build());
        }
    }
}
