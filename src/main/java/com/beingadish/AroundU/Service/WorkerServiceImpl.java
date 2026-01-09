package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Mappers.User.Worker.WorkerMapper;
import com.beingadish.AroundU.Models.WorkerModel;
import com.beingadish.AroundU.Repository.Worker.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkerServiceImpl implements WorkerService {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerMapper workerMapper;

    @Autowired
    private PasswordEncoder passwordEncoder; // For password hashing

    @Override
    public WorkerModel registerWorker(WorkerModel registerWorkerModel, String plainPassword) {

        // 1. Validate if email already exists
        if (workerRepository.existsByEmail(registerWorkerModel.getEmail())) {
            throw new RuntimeException("Email already exists: " + registerWorkerModel.getEmail());
        }

        // 2. Validate if phone number already exists
        if (workerRepository.existsByPhoneNumber(registerWorkerModel.getPhoneNumber())) {
            throw new RuntimeException("Phone number already exists: " + registerWorkerModel.getPhoneNumber());
        }

        // 3. Hash the password
        registerWorkerModel.setHashedPassword(passwordEncoder.encode(plainPassword));

        // 4. Set initial verification status
        if (registerWorkerModel.getVerificationStatus() == null) {
            // Set default verification status (you'll need to create this)
            // registerWorkerModel.setVerificationStatus(createDefaultVerificationStatus());
        }

        // 5. Convert Model to Entity
        Worker workerEntity = workerMapper.modelToEntity(registerWorkerModel);

        // 6. Save to database
        Worker savedEntity = workerRepository.save(workerEntity);

        // 7. Convert back to Model and return
        return workerMapper.entityToModel(savedEntity);
    }
}
