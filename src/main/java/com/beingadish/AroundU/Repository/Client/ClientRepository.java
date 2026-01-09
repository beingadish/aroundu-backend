package com.beingadish.AroundU.Repository.Client;

import com.beingadish.AroundU.Entities.Client;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends CrudRepository<Client, Long>, ClientReadRepository, ClientWriteRepository {
}