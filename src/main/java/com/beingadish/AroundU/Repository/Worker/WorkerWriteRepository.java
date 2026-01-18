package com.beingadish.AroundU.Repository.Worker;

import com.beingadish.AroundU.Entities.Worker;
import org.springframework.data.repository.Repository;

@org.springframework.stereotype.Repository
public interface WorkerWriteRepository extends Repository<Worker, Long> {
	Worker save(Worker worker);
	void deleteById(Long id);
	void deleteByEmail(String email);
}
