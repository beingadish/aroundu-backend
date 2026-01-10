package com.beingadish.AroundU.Repository.Client;

import com.beingadish.AroundU.Entities.Client;
import org.springframework.data.repository.Repository;

// Write-focused repository abstraction for Client
@org.springframework.stereotype.Repository
public interface ClientWriteRepository extends Repository<Client, Long> {
    Client save(Client client);
    void deleteById(Long id);
    void deleteByEmail(String email);
}
