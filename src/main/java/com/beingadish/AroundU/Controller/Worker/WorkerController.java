package com.beingadish.AroundU.Controller.Worker;

import com.beingadish.AroundU.DTO.Common.ApiResponse;
import com.beingadish.AroundU.DTO.Worker.Update.WorkerUpdateRequestDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
import com.beingadish.AroundU.Service.WorkerService;
import com.beingadish.AroundU.Utilities.PageResponse;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.beingadish.AroundU.Constants.URIConstants.REGISTER;
import static com.beingadish.AroundU.Constants.URIConstants.WORKER_BASE;

@RestController
@RequestMapping(WORKER_BASE)
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @PostMapping(REGISTER)
    public ResponseEntity<ApiResponse<String>> registerWorker(@Valid @RequestBody WorkerSignupRequestDTO workerSignupRequestDTO) {
        workerService.registerWorker(workerSignupRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Worker registered successfully"));
    }

    @GetMapping("/{workerId}")
    @PreAuthorize("hasRole('ADMIN') or #workerId == authentication.principal.id")
    public ResponseEntity<ApiResponse<WorkerDetailDTO>> getWorkerDetails(@PathVariable Long workerId) {
        WorkerDetailDTO workerDetailDTO = workerService.getWorkerDetails(workerId);
        return ResponseEntity.ok(ApiResponse.success(workerDetailDTO));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<WorkerDetailDTO>>> getAllWorkers(@RequestParam int page, @RequestParam int size) {
        Page<WorkerDetailDTO> workersPage = workerService.getAllWorkers(page, size);
        PageResponse<WorkerDetailDTO> response = new PageResponse<>(workersPage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/update/{workerId}")
    @PreAuthorize("#workerId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorkerDetailDTO>> updateWorkerDetails(@PathVariable Long workerId, @RequestBody WorkerUpdateRequestDTO updateRequestDetails) {
        WorkerDetailDTO updated = workerService.updateWorkerDetails(workerId, updateRequestDetails);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(updated));
    }
}
