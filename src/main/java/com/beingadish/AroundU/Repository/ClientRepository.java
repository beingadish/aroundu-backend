package com.beingadish.AroundU.Repository;

import com.beingadish.AroundU.Entities.ClientEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends CrudRepository<ClientEntity, Long> {

    @Query("SELECT c FROM ClientEntity c WHERE c.clientEmail = :email")
    Optional<ClientEntity> findByEmail(@Param("email") String email);

    @Query("SELECT c FROM ClientEntity c WHERE c.clientEmail = :email AND c.password = :password")
    Optional<ClientEntity> findByEmailAndPassword(@Param("email") String email, @Param("password") String password);
}
