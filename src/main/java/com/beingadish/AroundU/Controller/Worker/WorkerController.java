package com.beingadish.AroundU.Controller.Worker;

import com.beingadish.AroundU.DTO.Common.ApiResponse;
import com.beingadish.AroundU.DTO.Worker.Update.WorkerUpdateRequestDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
import com.beingadish.AroundU.Service.WorkerService;
import com.beingadish.AroundU.Utilities.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import static com.beingadish.AroundU.Constants.URIConstants.REGISTER;
import static com.beingadish.AroundU.Constants.URIConstants.WORKER_BASE;

@RestController
@RequestMapping(WORKER_BASE)
@RequiredArgsConstructor
@Tag(name = "Worker", description = "Worker registration, profile retrieval, pagination, and updates")
public class WorkerController {

    private final WorkerService workerService;

    @PostMapping(REGISTER)
    @Operation(summary = "Register worker", description = "Creates a worker account. Requires name, email, phoneNumber, password, currency, and a full currentAddress object.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Worker registered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<String>> registerWorker(@Valid @RequestBody WorkerSignupRequestDTO workerSignupRequestDTO) {
        workerService.registerWorker(workerSignupRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Worker registered successfully"));
    }

    @GetMapping("/{workerId}")
    @PreAuthorize("hasRole('ADMIN') or #workerId == authentication.principal.id")
    @Operation(summary = "Get worker details", description = "Fetch worker profile by id", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Worker found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<WorkerDetailDTO>> getWorkerDetails(@PathVariable Long workerId) {
        WorkerDetailDTO workerDetailDTO = workerService.getWorkerDetails(workerId);
        return ResponseEntity.ok(ApiResponse.success(workerDetailDTO));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "Get current worker details", description = "Fetch worker profile for the authenticated worker", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WorkerDetailDTO>> getMyWorkerDetails() {
        Long workerId = authenticationPrincipalId();
        WorkerDetailDTO details = workerService.getWorkerDetails(workerId);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @GetMapping("/all")
    @Operation(summary = "List workers", description = "Paged listing of workers (admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ApiResponse<PageResponse<WorkerDetailDTO>>> getAllWorkers(@RequestParam int page, @RequestParam int size) {
        Page<WorkerDetailDTO> workersPage = workerService.getAllWorkers(page, size);
        PageResponse<WorkerDetailDTO> response = new PageResponse<>(workersPage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/update/{workerId}")
    @PreAuthorize("#workerId == authentication.principal.id or hasRole('ADMIN')")
    @Operation(summary = "Update worker details", description = "Partial update of worker profile fields", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<WorkerDetailDTO>> updateWorkerDetails(@PathVariable Long workerId, @RequestBody WorkerUpdateRequestDTO updateRequestDetails) {
        WorkerDetailDTO updated = workerService.updateWorkerDetails(workerId, updateRequestDetails);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(updated));
    }

    private Long authenticationPrincipalId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (principal instanceof com.beingadish.AroundU.Security.UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        return Long.parseLong(authentication.getName());
    }

    @DeleteMapping("/{workerId}")
    @PreAuthorize("hasRole('ADMIN') or #workerId == authentication.principal.id")
    @Operation(summary = "Delete worker", description = "Deletes worker and related data", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<String>> deleteWorker(@PathVariable Long workerId) {
        workerService.deleteWorker(workerId);
        return ResponseEntity.ok(ApiResponse.success("Worker deleted successfully"));
    }
}
