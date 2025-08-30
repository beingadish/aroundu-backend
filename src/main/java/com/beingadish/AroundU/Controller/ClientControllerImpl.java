package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.Constants.URIConstants;
import com.beingadish.AroundU.Service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(URIConstants.CLIENT_BASE_MAPPING_URI_V1)
public class ClientControllerImpl implements ClientController {

    @Autowired
    private ClientService clientService;

    @Override
    @PostMapping("/register")
    public ResponseEntity<ClientResponseDTO> registerClient(@RequestBody ClientRequestDTO clientRequestDTO) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(clientService.registerClient(clientRequestDTO));
    }

    @Override
    @GetMapping("/getDetails")
    public ResponseEntity<ClientResponseDTO> getClientDetails(@RequestParam(value="email") String clientEmail) {
        ClientRequestDTO clientRequestDTO = ClientRequestDTO
                .builder()
                .clientEmail(clientEmail)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(clientService.getClientDetails(clientRequestDTO));
    }

    @Override
    @PutMapping("/updateDetails")
    public ResponseEntity<ClientResponseDTO> updateClientDetails(@RequestBody ClientRequestDTO clientRequestDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(clientService.updateClientDetails(clientRequestDTO));
    }
}