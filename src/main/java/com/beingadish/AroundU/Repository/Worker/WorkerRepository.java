package com.beingadish.AroundU.Repository.Worker;

import com.beingadish.AroundU.Entities.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
	boolean existsByEmail(String email);

	boolean existsByPhoneNumber(String phoneNumber);

	Optional<Worker> findByEmail(String email);

	Optional<Worker> findByPhoneNumber(String phoneNumber);
}
