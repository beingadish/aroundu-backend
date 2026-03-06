package com.beingadish.AroundU.chat.mapper;

import com.beingadish.AroundU.chat.dto.ChatMessageResponseDTO;
import com.beingadish.AroundU.chat.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    ChatMessageResponseDTO toDto(ChatMessage message);

    List<ChatMessageResponseDTO> toDtoList(List<ChatMessage> messages);
}
