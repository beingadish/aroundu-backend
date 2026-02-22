package com.beingadish.AroundU.user.controller;

import com.beingadish.AroundU.common.dto.ApiResponse;
import com.beingadish.AroundU.user.dto.PublicClientProfileDTO;
import com.beingadish.AroundU.user.dto.PublicWorkerProfileDTO;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.exception.ClientNotFoundException;
import com.beingadish.AroundU.user.exception.WorkerNotFoundException;
import com.beingadish.AroundU.user.repository.ClientReadRepository;
import com.beingadish.AroundU.user.repository.WorkerReadRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import static com.beingadish.AroundU.common.constants.URIConstants.PUBLIC_BASE;

@RestController
@RequestMapping(PUBLIC_BASE)
@RequiredArgsConstructor
@Tag(name = "Public Profile", description = "Public profile endpoints accessible to any authenticated user")
public class PublicProfileController {

    private final WorkerReadRepository workerReadRepository;
    private final ClientReadRepository clientReadRepository;

    @GetMapping("/worker/{workerId}")
    @Operation(summary = "Get public worker profile", description = "Returns non-sensitive worker profile information")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Worker profile found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Worker not found")
    })
    public ResponseEntity<ApiResponse<PublicWorkerProfileDTO>> getWorkerPublicProfile(
            @Parameter(description = "Worker ID", required = true) @PathVariable Long workerId) {
        Worker worker = workerReadRepository.findById(workerId)
                .orElseThrow(() -> new WorkerNotFoundException("Worker not found with id: " + workerId));

        PublicWorkerProfileDTO dto = PublicWorkerProfileDTO.builder()
                .id(worker.getId())
                .name(worker.getName())
                .profileImageUrl(worker.getProfileImageUrl())
                .overallRating(worker.getOverallRating())
                .experienceYears(worker.getExperienceYears())
                .certifications(worker.getCertifications())
                .isOnDuty(worker.getIsOnDuty())
                .skills(Collections.emptyList())
                .build();

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Get public client profile", description = "Returns non-sensitive client profile information")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Client profile found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<ApiResponse<PublicClientProfileDTO>> getClientPublicProfile(
            @Parameter(description = "Client ID", required = true) @PathVariable Long clientId) {
        Client client = clientReadRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + clientId));

        PublicClientProfileDTO dto = PublicClientProfileDTO.builder()
                .id(client.getId())
                .name(client.getName())
                .profileImageUrl(client.getProfileImageUrl())
                .build();

        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
