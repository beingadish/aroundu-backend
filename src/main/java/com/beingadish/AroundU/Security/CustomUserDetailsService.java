package com.beingadish.AroundU.Security;

import com.beingadish.AroundU.Entities.Admin;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Repository.Admin.AdminRepository;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerRepository;
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

    private final ClientRepository clientRepository;
    private final WorkerRepository workerRepository;
    private final AdminRepository adminRepository;

    private static final String ADMIN = "ROLE_ADMIN";
    private static final String CLIENT = "ROLE_CLIENT";
    private static final String WORKER = "ROLE_WORKER";

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try finding in Client repository
        Optional<Client> client = clientRepository.findByEmail(email);
        if (client.isPresent()) {
            return User.builder().username(client.get().getEmail()).password(client.get().getHashedPassword()).authorities(CLIENT).build();
        }

        // Try finding in Worker repository
        Optional<Worker> worker = workerRepository.findByEmail(email);
        if (worker.isPresent()) {
            return User.builder().username(worker.get().getEmail()).password(worker.get().getHashedPassword()).authorities(WORKER).build();
        }

        // Try finding in Admin repository
        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) {
            return User.builder()
                    .username(admin.get().getEmail())
                    .password(admin.get().getHashedPassword())
                    .authorities(ADMIN)
                    .build();
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
