package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface WorkerController {
    WorkerDetailDTO registerUser(WorkerSignupRequestDTO workerSignupRequestDTO);
}
