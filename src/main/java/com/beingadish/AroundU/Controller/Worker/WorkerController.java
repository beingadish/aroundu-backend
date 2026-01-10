package com.beingadish.AroundU.Controller.Worker;

import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
import com.beingadish.AroundU.Mappers.User.Worker.WorkerMapper;
import com.beingadish.AroundU.Models.WorkerModel;
import com.beingadish.AroundU.Service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.beingadish.AroundU.Constants.URIConstants.REGISTER;
import static com.beingadish.AroundU.Constants.URIConstants.WORKER_BASE;

@RestController
@RequestMapping(WORKER_BASE)
@RequiredArgsConstructor
public class WorkerController {

    private WorkerService workerService;
    private WorkerMapper mapper;

    @RequestMapping(REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkerDetailDTO registerWorker(@RequestBody WorkerSignupRequestDTO workerSignupRequestDTO) {
        WorkerModel newWorker = mapper.signupRequestDtoToModel(workerSignupRequestDTO);
        WorkerModel registeredWorker = workerService.registerWorker(newWorker, workerSignupRequestDTO.getPassword());
        return mapper.modelToWorkerDetailDto(registeredWorker);
    }
}
