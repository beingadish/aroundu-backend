package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.Constants.URIConstants;
import com.beingadish.AroundU.DTO.Worker.Signup.WorkerSignupRequestDTO;
import com.beingadish.AroundU.DTO.Worker.Signup.WorkerSignupResponseDTO;
import com.beingadish.AroundU.Service.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(URIConstants.WORKER_BASE_MAPPING_URI_V1)
public class WorkerControllerImpl implements WorkerController {

    @Autowired
    private WorkerService workerService;

    @Override
    @RequestMapping(URIConstants.REGISTER)
    public ResponseEntity<WorkerSignupResponseDTO> registerUser(@RequestBody WorkerSignupRequestDTO workerSignupRequestDTO) {

        return null;
    }
}
