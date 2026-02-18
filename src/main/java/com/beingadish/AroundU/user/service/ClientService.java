package com.beingadish.AroundU.user.service;

import com.beingadish.AroundU.user.dto.client.ClientDetailsResponseDTO;
import com.beingadish.AroundU.user.dto.client.ClientRegisterRequestDTO;
import com.beingadish.AroundU.user.dto.client.ClientUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public interface ClientService {

    // For registering the client
    void registerClient(ClientRegisterRequestDTO clientRequestDTO);

    // For getting the client details
    ClientDetailsResponseDTO getClientDetails(Long clientId);

    Page<ClientDetailsResponseDTO> getAllClients(int page, int size);

    ClientDetailsResponseDTO updateClientDetails(Long clientId, ClientUpdateRequestDTO updateRequest);

    void deleteClient(Long clientId);

}
