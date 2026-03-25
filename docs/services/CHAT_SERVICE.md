# Chat Service

> Job-scoped messaging between clients and workers with WebSocket support.

---

## Overview

The Chat module provides real-time messaging tied to specific jobs. Conversations are auto-created when a bid is accepted (see [BID_SERVICE.md](BID_SERVICE.md)). Supports message delivery/read receipts, paginated history, grouped views, WebSocket push, and automatic cleanup of completed/expired conversations.

**Package:** `com.beingadish.AroundU.chat`

---

## File Inventory

### Entities

| File | Description |
|------|-------------|
| `entity/Conversation.java` | Job-scoped conversation: `job`, `clientId`, `workerId`, `lastMessageAt`, `archivedAt`, unique constraint on (job, participants) |
| `entity/ChatMessage.java` | Message: `conversation`, `senderId`, `senderRole`, `content`, `status` (MessageStatus), `createdAt` |
| `entity/MessageStatus.java` | Enum: `SENT`, `DELIVERED`, `READ` |

### Repositories

| File | Description |
|------|-------------|
| `repository/ConversationRepository.java` | Find by job+participants, by participant, by job ID; archival queries |
| `repository/ChatMessageRepository.java` | Paginated messages, unread count, bulk status updates |

### Services

| File | Description |
|------|-------------|
| `service/ChatService.java` | Interface: 8 methods |
| `service/ChatServiceImpl.java` | Full implementation with validation and auto-conversation creation |

### Controllers

| File | Description |
|------|-------------|
| `controller/ChatController.java` | 6 REST endpoints (uses JWT principal for auth) |
| `controller/ChatWebSocketController.java` | STOMP WebSocket controller for real-time messaging |

### DTOs

| File | Description |
|------|-------------|
| `dto/ChatMessageRequest.java` | `recipientId`, `content` |
| `dto/ChatMessageResponseDTO.java` | id, conversationId, senderId, senderRole, content, status, createdAt |
| `dto/ConversationResponseDTO.java` | id, jobId, jobTitle, participant names, unreadCount, lastMessageAt |
| `dto/JobConversationsDTO.java` | Grouped view: jobId, jobTitle, list of conversations |
| `dto/MessageStatusUpdate.java` | WebSocket status update payload |
| `dto/TypingEvent.java` | WebSocket typing indicator payload |

---

## Service Methods

### `ChatService`

| Method | Description |
|--------|-------------|
| `sendMessage(jobId, senderId, senderRole, request)` | Send message; auto-creates conversation if needed |
| `getMessages(conversationId, userId, page, size)` | Paginated messages (newest first) |
| `getConversations(userId, userRole)` | Flat list of conversations for a user |
| `getConversationsGroupedByJob(userId, userRole)` | Conversations grouped by job (for clients) |
| `markAsDelivered(conversationId, userId, userRole)` | Mark all messages as DELIVERED |
| `markAsRead(conversationId, userId, userRole)` | Mark all messages as READ |
| `archiveCompletedConversations()` | Archive conversations for completed/cancelled jobs |
| `deleteExpiredConversations()` | Delete conversations archived > 30 days |

---

## REST Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/chat/jobs/{jobId}/messages` | Authenticated | Send message |
| `GET` | `/api/v1/chat/conversations/{id}/messages` | Authenticated | Get paginated messages |
| `GET` | `/api/v1/chat/conversations` | Authenticated | List user's conversations |
| `GET` | `/api/v1/chat/conversations/grouped` | Authenticated | Conversations grouped by job |
| `POST` | `/api/v1/chat/conversations/{id}/delivered` | Authenticated | Mark messages as delivered |
| `POST` | `/api/v1/chat/conversations/{id}/read` | Authenticated | Mark messages as read |

---

## WebSocket Endpoints

| Destination | Description |
|-------------|-------------|
| `/app/chat.send` | Send a message via WebSocket |
| `/app/chat.typing` | Broadcast typing indicator |
| `/topic/conversation.{id}` | Subscribe to conversation messages |
| `/topic/conversation.{id}.status` | Subscribe to delivery/read status updates |

---

## Message Status Flow

```
SENT → DELIVERED → READ
  │        │         │
  │        │         └─ User opens conversation (POST /read)
  │        └─ App receives message (POST /delivered)
  └─ Message created (POST /messages)
```

---

## Scheduled Cleanup

| Scheduler | Schedule | Action |
|-----------|----------|--------|
| `ChatCleanupScheduler` | Daily at 3 AM | Archives completed conversations; deletes expired ones (>30 days) |

---

## Business Rules

1. **Sender must be a job participant** — validated against conversation's clientId/workerId
2. **Conversation dedup** — if conversation already exists for (job, client, worker), reuses it
3. **Conversation auto-creation on bid acceptance** — fail-safe (see BidServiceImpl)
4. **Principal-based auth** — controller extracts userId and role from JWT SecurityContext

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `ConversationRepository` | Conversation persistence |
| `ChatMessageRepository` | Message persistence |
| `JobRepository` | Job validation |
| `ClientReadRepository` / `WorkerReadRepository` | Name resolution for DTOs |
