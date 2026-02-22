package com.beingadish.AroundU.infrastructure.interceptor;

import com.beingadish.AroundU.infrastructure.security.UserPrincipal;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.User;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.repository.AdminRepository;
import com.beingadish.AroundU.user.repository.ClientReadRepository;
import com.beingadish.AroundU.user.repository.WorkerReadRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Interceptor that extracts the authenticated user from the
 * {@link SecurityContextHolder}, loads full user details from the database, and
 * stores the user entity as a request attribute so controllers can access it
 * via {@code request.getAttribute("currentUser")}.
 * <p>
 * Anonymous / unauthenticated requests are handled gracefully — no error is
 * thrown.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ATTR = "currentUser";
    public static final String CURRENT_USER_ROLE_ATTR = "currentUserRole";

    private final ClientReadRepository clientReadRepository;
    private final WorkerReadRepository workerReadRepository;
    private final AdminRepository adminRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authenticated user for request {}", request.getRequestURI());
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            // Could be an anonymous token or a simple String "anonymousUser"
            log.debug("Principal is not a UserPrincipal: {}", principal.getClass().getSimpleName());
            return true;
        }

        String email = userPrincipal.getEmail();
        String role = extractRole(authentication);

        try {
            Optional<? extends User> user = loadUser(email, role);
            user.ifPresent(u -> {
                request.setAttribute(CURRENT_USER_ATTR, u);
                request.setAttribute(CURRENT_USER_ROLE_ATTR, role);
                log.debug("User context loaded: id={}, email={}, role={}", u.getId(), email, role);
            });

            if (user.isEmpty()) {
                log.warn("Authenticated user '{}' with role '{}' not found in database", email, role);
            }
        } catch (Exception ex) {
            log.error("Failed to load user context for '{}': {}", email, ex.getMessage());
            // Do not block the request — let it proceed without user context
        }

        return true;
    }

    private Optional<? extends User> loadUser(String email, String role) {
        return switch (role) {
            case "ROLE_CLIENT" -> clientReadRepository.findByEmail(email).map(c -> (User) c);
            case "ROLE_WORKER" -> workerReadRepository.findByEmail(email).map(w -> (User) w);
            case "ROLE_ADMIN" -> adminRepository.findByEmail(email).map(a -> (User) a);
            default -> {
                log.warn("Unknown role '{}' — attempting to find user across all repositories", role);
                Optional<Client> client = clientReadRepository.findByEmail(email);
                if (client.isPresent()) {
                    yield client.map(c -> (User) c);
                }
                Optional<Worker> worker = workerReadRepository.findByEmail(email);
                if (worker.isPresent()) {
                    yield worker.map(w -> (User) w);
                }
                yield adminRepository.findByEmail(email).map(a -> (User) a);
            }
        };
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(a -> a.startsWith("ROLE_")).findFirst().orElse("UNKNOWN");
    }
}
