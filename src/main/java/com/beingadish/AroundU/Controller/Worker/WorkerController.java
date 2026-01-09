package com.beingadish.AroundU.Controller.Worker;

import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;

public interface WorkerController {
    WorkerDetailDTO registerUser(WorkerSignupRequestDTO workerSignupRequestDTO);
}
