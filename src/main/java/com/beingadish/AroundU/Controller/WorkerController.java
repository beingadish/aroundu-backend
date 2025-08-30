package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.DTO.Worker.Signup.WorkerSignupRequestDTO;
import com.beingadish.AroundU.DTO.Worker.Signup.WorkerSignupResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface WorkerController {

    ResponseEntity<WorkerSignupResponseDTO> registerUser(@RequestBody WorkerSignupRequestDTO workerSignupRequestDTO);
}
