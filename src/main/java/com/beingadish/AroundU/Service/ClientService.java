package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ClientService {

    // For registering the client
    ClientRegisterResponseDTO registerClient(ClientRegisterRequestDTO clientRequestDTO);

    // For getting the client details
    ClientDetailsResponseDTO getClientDetails(Long clientId);

    ResponseEntity<List<ClientDetailsResponseDTO>> getAllClients();

//    ClientResponseDTO updateClientDetails(ClientRequestDTO clientRequestDTO);

}
