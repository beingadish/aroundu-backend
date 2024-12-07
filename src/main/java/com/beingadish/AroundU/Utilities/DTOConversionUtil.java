package com.beingadish.AroundU.Utilities;

import com.beingadish.AroundU.DTO.Client.ClientRequestDTO;
import com.beingadish.AroundU.DTO.Client.ClientResponseDTO;
import com.beingadish.AroundU.Entities.ClientEntity;

public class DTOConversionUtil {

    public static ClientResponseDTO clientRequestDtoToClientResponseDto(ClientRequestDTO clientRequestDTO, String responseMessage) {
        return ClientResponseDTO
                .builder()
                .message(responseMessage)
                .clientId(clientRequestDTO.getClientId())
                .clientName(clientRequestDTO.getClientName())
                .clientEmail(clientRequestDTO.getClientEmail())
                .password(clientRequestDTO.getPassword())
                .build();
    }

    public static ClientRequestDTO clientResponseDtoToClientRequestDto(ClientResponseDTO clientResponseDTO) {
        return ClientRequestDTO.builder().clientId(clientResponseDTO.getClientId()).clientName(clientResponseDTO.getClientName()).clientEmail(clientResponseDTO.getClientEmail()).password(clientResponseDTO.getPassword()).build();
    }

    public static ClientEntity clientRequestDtoToClientEntity(ClientRequestDTO clientRequestDTO) {
        return ClientEntity
                .builder()
                .clientName(clientRequestDTO.getClientName())
                .clientEmail(clientRequestDTO.getClientEmail())
                .clientId(clientRequestDTO.getClientId())
                .password(clientRequestDTO.getPassword())
                .build();
    }

    public static ClientResponseDTO clientEntityToClientResponseDto(ClientEntity clientEntity, String responseMessage) {
        return ClientResponseDTO.builder()
                .message(responseMessage)
                .clientId(clientEntity.getClientId())
                .clientName(clientEntity.getClientName())
                .clientEmail(clientEntity.getClientEmail())
                .password(clientEntity.getPassword())
                .build();
    }

}
