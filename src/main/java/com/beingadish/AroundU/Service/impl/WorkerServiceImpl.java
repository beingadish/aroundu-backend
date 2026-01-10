package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.DTO.Worker.Update.WorkerUpdateRequestDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Exceptions.Worker.WorkerAlreadyExistException;
import com.beingadish.AroundU.Exceptions.Worker.WorkerNotFoundException;
import com.beingadish.AroundU.Exceptions.Worker.WorkerValidationException;
import com.beingadish.AroundU.Mappers.User.Worker.WorkerMapper;
import com.beingadish.AroundU.Models.WorkerModel;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerWriteRepository;
import com.beingadish.AroundU.Service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final WorkerMapper workerMapper;
    private final WorkerReadRepository workerReadRepository;
    private final WorkerWriteRepository workerWriteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void registerWorker(WorkerSignupRequestDTO workerSignupRequestDTO) {
        WorkerModel workerModel = workerMapper.signupRequestDtoToModel(workerSignupRequestDTO);

        if (Boolean.TRUE.equals(workerReadRepository.existsByEmail(workerModel.getEmail()))) {
            throw new WorkerAlreadyExistException("Worker with the given email already exists");
        }

        if (Boolean.TRUE.equals(workerReadRepository.existsByPhoneNumber(workerModel.getPhoneNumber()))) {
            throw new WorkerAlreadyExistException("Worker with the given phone number already exists");
        }

        workerModel.setHashedPassword(passwordEncoder.encode(workerSignupRequestDTO.getPassword()));
        workerWriteRepository.save(workerMapper.modelToEntity(workerModel));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerDetailDTO getWorkerDetails(Long workerId) {
        Worker workerEntity = workerReadRepository.findById(workerId)
                .orElseThrow(() -> new WorkerNotFoundException("Worker with id %d does not exist".formatted(workerId)));

        WorkerModel model = workerMapper.toModel(workerEntity);
        return workerMapper.modelToWorkerDetailDto(model);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkerDetailDTO> getAllWorkers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Worker> pageData = workerReadRepository.findAll(pageable);
        return pageData.map(workerMapper::toModel).map(workerMapper::modelToWorkerDetailDto);
    }

    @Override
    @Transactional
    public WorkerDetailDTO updateWorkerDetails(Long workerId, WorkerUpdateRequestDTO updateRequest) {
        Worker foundWorker = workerReadRepository.findById(workerId)
                .orElseThrow(() -> new WorkerValidationException("Cannot update, worker not found"));

        if (updateRequest.getName() != null) {
            foundWorker.setName(updateRequest.getName());
        }

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(foundWorker.getEmail())) {
            if (Boolean.TRUE.equals(workerReadRepository.existsByEmail(updateRequest.getEmail()))) {
                throw new WorkerValidationException("Email already in use");
            }
            foundWorker.setEmail(updateRequest.getEmail());
        }

        if (updateRequest.getPhoneNumber() != null && !updateRequest.getPhoneNumber().equals(foundWorker.getPhoneNumber())) {
            if (Boolean.TRUE.equals(workerReadRepository.existsByPhoneNumber(updateRequest.getPhoneNumber()))) {
                throw new WorkerValidationException("Phone number already in use");
            }
            foundWorker.setPhoneNumber(updateRequest.getPhoneNumber());
        }

        if (updateRequest.getProfileImageUrl() != null) {
            foundWorker.setProfileImageUrl(updateRequest.getProfileImageUrl());
        }

        if (updateRequest.getExperienceYears() != null) {
            foundWorker.setExperienceYears(updateRequest.getExperienceYears());
        }

        if (updateRequest.getCertifications() != null) {
            foundWorker.setCertifications(updateRequest.getCertifications());
        }

        if (updateRequest.getIsOnDuty() != null) {
            foundWorker.setIsOnDuty(updateRequest.getIsOnDuty());
        }

        if (updateRequest.getPayoutAccount() != null) {
            foundWorker.setPayoutAccount(updateRequest.getPayoutAccount());
        }

        if (updateRequest.getCurrency() != null) {
            foundWorker.setCurrency(updateRequest.getCurrency());
        }

        Worker updatedWorker = workerWriteRepository.save(foundWorker);
        WorkerModel updatedModel = workerMapper.toModel(updatedWorker);
        return workerMapper.modelToWorkerDetailDto(updatedModel);
    }
}
