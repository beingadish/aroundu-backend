package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.DTO.Client.register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.register.ClientRegisterResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


public interface ClientController {

    // For registering the client
    ResponseEntity<ClientRegisterResponseDTO> registerClient(@RequestBody ClientRegisterRequestDTO clientRequestDTO);
    // For sending the client details
    ResponseEntity<ClientResponseDTO> getClientDetails(@RequestParam("clientEmail") String clientEmail);


    ResponseEntity<ClientResponseDTO> updateClientDetails(@RequestBody ClientRequestDTO clientRequestDTO);
}
