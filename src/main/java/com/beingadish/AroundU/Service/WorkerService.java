package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.DTO.Worker.Update.WorkerUpdateRequestDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
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
