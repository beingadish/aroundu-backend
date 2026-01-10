package com.beingadish.AroundU.Repository.Client;

import com.beingadish.AroundU.Entities.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ClientReadRepository extends Repository<Client, Long> {
    Optional<Client> findById(Long id);
    Optional<Client> findByEmail(String email);
    Boolean existsByEmail(String email);
    @Query(value = "SELECT c FROM Client c", countQuery = "SELECT count(c) FROM Client c")
    Page<Client> findAll(Pageable pageable);
}
