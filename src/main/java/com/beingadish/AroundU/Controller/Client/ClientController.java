package com.beingadish.AroundU.Controller.Client;

import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.Update.ClientUpdateRequestDTO;
import com.beingadish.AroundU.DTO.Common.ApiResponse;
import com.beingadish.AroundU.Service.ClientService;
import com.beingadish.AroundU.Utilities.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import static com.beingadish.AroundU.Constants.URIConstants.CLIENT_BASE;
import static com.beingadish.AroundU.Constants.URIConstants.REGISTER;

@RestController
@RequestMapping(CLIENT_BASE)
@RequiredArgsConstructor
@Tag(name = "Client", description = "Client registration, profile retrieval, pagination, and updates")
public class ClientController {

    private final ClientService clientService;

    @PostMapping(REGISTER)
    @Operation(summary = "Register client", description = "Creates a client account. Requires name, email, phoneNumber, password, currency, and a full currentAddress object.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Client registered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<String>> registerClient(@Valid @RequestBody ClientRegisterRequestDTO request) {
        clientService.registerClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Client registered successfully"));
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN') or #clientId == authentication.principal.id")
    @Operation(summary = "Get client details", description = "Fetch client profile by id", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Client found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<ClientDetailsResponseDTO>> getClientDetails(@PathVariable Long clientId) {
        ClientDetailsResponseDTO details = clientService.getClientDetails(clientId);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get current client details", description = "Fetch client profile for the authenticated client", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ClientDetailsResponseDTO>> getMyClientDetails() {
        Long clientId = authenticationPrincipalId();
        ClientDetailsResponseDTO details = clientService.getClientDetails(clientId);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    private Long authenticationPrincipalId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (principal instanceof com.beingadish.AroundU.Security.UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        return Long.parseLong(authentication.getName());
    }

    @GetMapping("/all")
    @Operation(summary = "List clients", description = "Paged listing of clients (admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ApiResponse<PageResponse<ClientDetailsResponseDTO>>> getAllClients(@RequestParam int page, @RequestParam int size) {
        Page<ClientDetailsResponseDTO> p = clientService.getAllClients(page, size);
        PageResponse<ClientDetailsResponseDTO> response = new PageResponse<>(p);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/update/{clientId}")
    @PreAuthorize("#clientId == authentication.principal.id or hasRole('ADMIN')")
    @Operation(summary = "Update client details", description = "Partial update of client profile fields", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<ClientDetailsResponseDTO>> updateClientDetails(@PathVariable Long clientId, @RequestBody ClientUpdateRequestDTO updateRequestDetails) {
        ClientDetailsResponseDTO updated = clientService.updateClientDetails(clientId, updateRequestDetails);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(updated));
    }

    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN') or #clientId == authentication.principal.id")
    @Operation(summary = "Delete client", description = "Deletes client and related data", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<String>> deleteClient(@PathVariable Long clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client deleted successfully"));
    }
}