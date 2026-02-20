package com.beingadish.AroundU.user.repository;

import com.beingadish.AroundU.user.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    @Query("SELECT c FROM Client c WHERE c.deleted = false AND c.lastLoginAt < :cutoff")
    List<Client> findInactiveClientsBefore(@Param("cutoff") LocalDateTime cutoff);
}
