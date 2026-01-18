package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.Update.ClientUpdateRequestDTO;
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
