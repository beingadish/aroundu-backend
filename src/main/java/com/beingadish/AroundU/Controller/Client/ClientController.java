package com.beingadish.AroundU.Controller.Client;

import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


public interface ClientController {

    // For registering the client
    ResponseEntity<ClientRegisterResponseDTO> registerClient(@RequestBody ClientRegisterRequestDTO clientRequestDTO);

    // For sending the client details
    ResponseEntity<ClientDetailsResponseDTO> getClientDetails(Long clientId);

    // Fetch all clients
    ResponseEntity<List<ClientDetailsResponseDTO>> getAllClients();
//    ResponseEntity<ClientResponseDTO> getClientDetailsLevel2(@RequestParam("clientEmail") String clientEmail);

//    ResponseEntity<ClientResponseDTO> getClientDetailsLevel3(@RequestParam("clientEmail") String clientEmail);
//
//
//    ResponseEntity<ClientResponseDTO> updateClientDetails(@RequestBody ClientRequestDTO clientRequestDTO);
}
