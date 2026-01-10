package com.beingadish.AroundU.Security;

import com.beingadish.AroundU.Entities.Admin;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Repository.Admin.AdminRepository;
import com.beingadish.AroundU.Repository.Client.ClientReadRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final String ADMIN = "ROLE_ADMIN";
    private static final String CLIENT = "ROLE_CLIENT";
    private static final String WORKER = "ROLE_WORKER";
    private final ClientReadRepository clientRepository;
    private final WorkerReadRepository workerRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Try to check for client
        Optional<Client> client = findClient(identifier);
        if (client.isPresent()) {
            return User.builder().username(client.get().getEmail()).password(client.get().getHashedPassword()).authorities(CLIENT).build();
        }

        // Try to check for worker
        Optional<Worker> worker = findWorker(identifier);
        if (worker.isPresent()) {
            return User.builder().username(worker.get().getEmail()).password(worker.get().getHashedPassword()).authorities(WORKER).build();
        }

        // Try to check for admin
        Optional<Admin> admin = findAdmin(identifier);
        if (admin.isPresent()) {
            return User.builder().username(admin.get().getEmail()).password(admin.get().getHashedPassword()).authorities(ADMIN).build();
        }

        throw new UsernameNotFoundException("User not found with identifier: " + identifier);
    }

    private Optional<Client> findClient(String identifier) {
        try {
            return clientRepository.findById(Long.parseLong(identifier));
        } catch (NumberFormatException ex) {
            return clientRepository.findByEmail(identifier);
        }
    }

    private Optional<Worker> findWorker(String identifier) {
        try {
            Long id = Long.parseLong(identifier);
            return workerRepository.findById(id);
        } catch (NumberFormatException ex) {
            return workerRepository.findByEmail(identifier);
        }
    }

    private Optional<Admin> findAdmin(String identifier) {
        try {
            Long id = Long.parseLong(identifier);
            return adminRepository.findById(id);
        } catch (NumberFormatException ex) {
            return adminRepository.findByEmail(identifier);
        }
    }
}
