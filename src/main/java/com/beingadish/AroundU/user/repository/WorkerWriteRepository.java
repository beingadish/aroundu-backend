package com.beingadish.AroundU.user.repository;

import com.beingadish.AroundU.user.entity.Worker;
import org.springframework.data.repository.Repository;

@org.springframework.stereotype.Repository
public interface WorkerWriteRepository extends Repository<Worker, Long> {
    Worker save(Worker worker);

    void deleteById(Long id);

    void deleteByEmail(String email);
}
