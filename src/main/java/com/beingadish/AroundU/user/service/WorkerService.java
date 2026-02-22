package com.beingadish.AroundU.user.service;

import com.beingadish.AroundU.user.dto.worker.WorkerDetailDTO;
import com.beingadish.AroundU.user.dto.worker.WorkerSignupRequestDTO;
import com.beingadish.AroundU.user.dto.worker.WorkerUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public interface WorkerService {
    void registerWorker(WorkerSignupRequestDTO workerSignupRequestDTO);

    WorkerDetailDTO getWorkerDetails(Long workerId);

    Page<WorkerDetailDTO> getAllWorkers(int page, int size);

    WorkerDetailDTO updateWorkerDetails(Long workerId, WorkerUpdateRequestDTO updateRequest);

    void deleteWorker(Long workerId);
}
