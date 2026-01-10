package com.beingadish.AroundU.Controller.Client;

import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.Update.ClientUpdateRequestDTO;
import com.beingadish.AroundU.DTO.Common.ApiResponse;
import com.beingadish.AroundU.Service.ClientService;
import com.beingadish.AroundU.Utilities.PageResponse;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.beingadish.AroundU.Constants.URIConstants.CLIENT_BASE;
import static com.beingadish.AroundU.Constants.URIConstants.REGISTER;

@RestController
@RequestMapping(CLIENT_BASE)
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping(REGISTER)
    public ResponseEntity<ApiResponse<String>> registerClient(@Valid @RequestBody ClientRegisterRequestDTO request) {
        clientService.registerClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Client registered successfully"));
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN') or #clientId == authentication.principal.id")
    public ResponseEntity<ApiResponse<ClientDetailsResponseDTO>> getClientDetails(@PathVariable Long clientId) {
        ClientDetailsResponseDTO details = clientService.getClientDetails(clientId);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<ClientDetailsResponseDTO>>> getAllClients(@RequestParam int page, @RequestParam int size) {
        Page<ClientDetailsResponseDTO> p = clientService.getAllClients(page, size);
        PageResponse<ClientDetailsResponseDTO> response = new PageResponse<>(p);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/update/{clientId}")
    @PreAuthorize("#clientId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ClientDetailsResponseDTO>> updateClientDetails(@PathVariable Long clientId, @RequestBody ClientUpdateRequestDTO updateRequestDetails) {
        ClientDetailsResponseDTO updated = clientService.updateClientDetails(clientId, updateRequestDetails);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(updated));
    }
}