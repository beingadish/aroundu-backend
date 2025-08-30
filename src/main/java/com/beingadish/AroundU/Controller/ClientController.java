package com.beingadish.AroundU.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


public interface ClientController {


    ResponseEntity<ClientResponseDTO> registerClient(@RequestBody ClientRequestDTO clientRequestDTO);


    ResponseEntity<ClientResponseDTO> getClientDetails(@RequestParam("clientEmail") String clientEmail);


    ResponseEntity<ClientResponseDTO> updateClientDetails(@RequestBody ClientRequestDTO clientRequestDTO);
}
