package com.beingadish.AroundU.chat.controller;

import com.beingadish.AroundU.chat.dto.ChatMessageRequest;
import com.beingadish.AroundU.chat.dto.ChatMessageResponseDTO;
import com.beingadish.AroundU.chat.dto.ConversationResponseDTO;
import com.beingadish.AroundU.chat.service.ChatService;
import com.beingadish.AroundU.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.beingadish.AroundU.common.constants.URIConstants.CHAT_BASE;

@RestController
@RequestMapping(CHAT_BASE)
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Job-scoped messaging between client and worker")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/jobs/{jobId}/messages")
    @Operation(summary = "Send message", description = "Send a chat message within a job conversation")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Message sent",
                content = @Content(schema = @Schema(implementation = ChatMessageResponseDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<ApiResponse<ChatMessageResponseDTO>> sendMessage(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId,
            @Parameter(description = "Sender user ID", required = true) @RequestParam Long senderId,
            @Valid @RequestBody ChatMessageRequest request) {
        ChatMessageResponseDTO dto = chatService.sendMessage(jobId, senderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get conversation messages", description = "Retrieve paginated messages for a conversation")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Messages retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not a participant"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    public ResponseEntity<ApiResponse<List<ChatMessageResponseDTO>>> getMessages(
            @Parameter(description = "Conversation ID", required = true) @PathVariable Long conversationId,
            @Parameter(description = "User ID (must be a participant)", required = true) @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<ChatMessageResponseDTO> messages = chatService.getMessages(conversationId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List conversations", description = "Get all conversations for the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversations listed")
    })
    public ResponseEntity<ApiResponse<List<ConversationResponseDTO>>> getConversations(
            @Parameter(description = "User ID", required = true) @RequestParam Long userId) {
        List<ConversationResponseDTO> conversations = chatService.getConversations(userId);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @PostMapping("/conversations/{conversationId}/read")
    @Operation(summary = "Mark as read", description = "Mark all messages in a conversation as read for the user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Messages marked as read"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @Parameter(description = "Conversation ID", required = true) @PathVariable Long conversationId,
            @Parameter(description = "User ID", required = true) @RequestParam Long userId) {
        chatService.markAsRead(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read"));
    }
}
