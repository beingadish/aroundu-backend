package com.beingadish.AroundU.Controller.Client;

import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterResponseDTO;
import com.beingadish.AroundU.Service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.beingadish.AroundU.Constants.URIConstants.CLIENT_BASE;
import static com.beingadish.AroundU.Constants.URIConstants.REGISTER;

@RestController
@RequestMapping(CLIENT_BASE)
@RequiredArgsConstructor
public class ClientController {

    private ClientService clientService;

    @PostMapping(REGISTER)
    public ResponseEntity<ClientRegisterResponseDTO> registerClient(@RequestBody ClientRegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.registerClient(request));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientDetailsResponseDTO> getClientDetails(@PathVariable Long clientId) {
        ClientDetailsResponseDTO clientDetails = clientService.getClientDetails(clientId);
        return ResponseEntity.ok(clientDetails);
    }

    public ResponseEntity<List<ClientDetailsResponseDTO>> getAllClients() {
        return clientService.getAllClients();
    }
//
//    @Override
//    @GetMapping("/getDetails")
//    public ResponseEntity<ClientResponseDTO> getClientDetails(@RequestParam(value="email") String clientEmail) {
//        ClientRequestDTO clientRequestDTO = ClientRequestDTO
//                .builder()
//                .clientEmail(clientEmail)
//                .build();
//        return ResponseEntity.status(HttpStatus.OK).body(clientService.getClientDetails(clientRequestDTO));
//    }
//
//    @Override
//    @PutMapping("/updateDetails")
//    public ResponseEntity<ClientResponseDTO> updateClientDetails(@RequestBody ClientRequestDTO clientRequestDTO) {
//        return ResponseEntity.status(HttpStatus.OK).body(clientService.updateClientDetails(clientRequestDTO));
//    }
}