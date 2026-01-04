package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.Constants.URIConstants;
import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
import com.beingadish.AroundU.Mappers.WorkerMapper;
import com.beingadish.AroundU.Models.WorkerModel;
import com.beingadish.AroundU.Service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(URIConstants.WORKER_BASE_MAPPING_URI_V1)
@RequiredArgsConstructor
public class WorkerControllerImpl implements WorkerController {

    private WorkerService workerService;

    private WorkerMapper mapper;

    @Override
    @RequestMapping(URIConstants.REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkerDetailDTO registerUser(@RequestBody WorkerSignupRequestDTO workerSignupRequestDTO) {
            WorkerModel newWorker = mapper.signupRequestDtoToModel(workerSignupRequestDTO);
            WorkerModel registeredWorker = workerService.registerWorker(newWorker, workerSignupRequestDTO.getPassword());
            return mapper.modelToWorkerDetailDto(registeredWorker);
    }
}
