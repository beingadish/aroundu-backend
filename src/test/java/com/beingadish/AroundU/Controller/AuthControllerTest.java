package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import com.beingadish.AroundU.Controller.Auth.AuthController;
import com.beingadish.AroundU.DTO.Auth.LoginRequestDTO;
import com.beingadish.AroundU.Entities.Admin;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Repository.Admin.AdminRepository;
import com.beingadish.AroundU.Repository.Client.ClientReadRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import com.beingadish.AroundU.Security.JwtTokenProvider;
import com.beingadish.AroundU.Security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(TestWebSecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
    + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
    + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
    "spring.data.jpa.repositories.enabled=false"
})
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private JwtTokenProvider tokenProvider;
    @MockitoBean
    private ClientReadRepository clientReadRepository;
    @MockitoBean
    private WorkerReadRepository workerReadRepository;
    @MockitoBean
    private AdminRepository adminRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Security.JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── JPA infrastructure mocks (no DB in WebMvcTest slice) ────
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Client.ClientWriteRepository clientWriteRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Client.ClientRepository clientRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Worker.WorkerWriteRepository workerWriteRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Worker.WorkerRepository workerRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Skill.SkillRepository skillRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Job.JobRepository jobRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Job.JobConfirmationCodeRepository jobConfirmationCodeRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Bid.BidRepository bidRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Payment.PaymentTransactionRepository paymentTransactionRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Address.AddressRepository addressRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.FailedGeoSync.FailedGeoSyncRepository failedGeoSyncRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Analytics.AggregatedMetricsRepository aggregatedMetricsRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Notification.FailedNotificationRepository failedNotificationRepository;
    @SuppressWarnings("unused")
    @MockitoBean(name = "entityManagerFactory")
    private EntityManagerFactory entityManagerFactory;
    @SuppressWarnings("unused")
    @MockitoBean(name = "jpaSharedEM_entityManagerFactory")
    private jakarta.persistence.EntityManager sharedEntityManager;
    @SuppressWarnings("unused")
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @SuppressWarnings("unused")
    @MockitoBean
    private PlatformTransactionManager platformTransactionManager;

    private static final String LOGIN_URL = "/api/v1/auth/login";

    // ── helpers ──────────────────────────────────────────────────
    private LoginRequestDTO loginRequest(String email, String password) {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private Authentication authResult(Long id, String email, String role, String password) {
        UserPrincipal principal = UserPrincipal.builder()
                .id(id).email(email).password(password)
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
        return new UsernamePasswordAuthenticationToken(principal, password, principal.getAuthorities());
    }

    // ── Successful Login ─────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/auth/login – success")
    class LoginSuccess {

        @Test
        @DisplayName("200 OK – client login returns JWT")
        void clientLogin() throws Exception {
            LoginRequestDTO req = loginRequest("client@test.com", "password");
            when(authenticationManager.authenticate(any())).thenReturn(authResult(1L, req.getEmail(), "ROLE_CLIENT", req.getPassword()));
            when(clientReadRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(clientEntity(1L, req.getEmail())));
            when(tokenProvider.generateToken(1L, req.getEmail(), "ROLE_CLIENT")).thenReturn("jwt-client");

            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.token").value("jwt-client"))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.role").value("ROLE_CLIENT"));
        }

        @Test
        @DisplayName("200 OK – worker login returns JWT")
        void workerLogin() throws Exception {
            LoginRequestDTO req = loginRequest("worker@test.com", "password");
            when(authenticationManager.authenticate(any())).thenReturn(authResult(10L, req.getEmail(), "ROLE_WORKER", req.getPassword()));
            when(workerReadRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(workerEntity(10L, req.getEmail())));
            when(tokenProvider.generateToken(10L, req.getEmail(), "ROLE_WORKER")).thenReturn("jwt-worker");

            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(10))
                    .andExpect(jsonPath("$.role").value("ROLE_WORKER"));
        }

        @Test
        @DisplayName("200 OK – admin login returns JWT")
        void adminLogin() throws Exception {
            LoginRequestDTO req = loginRequest("admin@test.com", "password");
            when(authenticationManager.authenticate(any())).thenReturn(authResult(99L, req.getEmail(), "ROLE_ADMIN", req.getPassword()));
            when(adminRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(adminEntity(99L, req.getEmail())));
            when(tokenProvider.generateToken(99L, req.getEmail(), "ROLE_ADMIN")).thenReturn("jwt-admin");

            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(99))
                    .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
        }
    }

    // ── Validation Errors ────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/auth/login – validation")
    class LoginValidation {

        @Test
        @DisplayName("400 – missing email")
        void missingEmail() throws Exception {
            LoginRequestDTO req = loginRequest(null, "password");
            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – missing password")
        void missingPassword() throws Exception {
            LoginRequestDTO req = loginRequest("client@test.com", null);
            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – invalid email format")
        void invalidEmail() throws Exception {
            LoginRequestDTO req = loginRequest("not-an-email", "password");
            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── Authentication Failures ──────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/auth/login – failures")
    class LoginFailures {

        @Test
        @DisplayName("401 – invalid credentials")
        void invalidCredentials() throws Exception {
            LoginRequestDTO req = loginRequest("client@test.com", "wrong");
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 – user not found during resolution")
        void userNotFound() throws Exception {
            LoginRequestDTO req = loginRequest("unknown@test.com", "password");
            when(authenticationManager.authenticate(any())).thenReturn(authResult(1L, req.getEmail(), "ROLE_CLIENT", req.getPassword()));
            when(clientReadRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ── entity helpers ───────────────────────────────────────────
    private Client clientEntity(Long id, String email) {
        Client c = new Client();
        c.setId(id);
        c.setEmail(email);
        return c;
    }

    private Worker workerEntity(Long id, String email) {
        Worker w = new Worker();
        w.setId(id);
        w.setEmail(email);
        return w;
    }

    private Admin adminEntity(Long id, String email) {
        Admin a = new Admin();
        a.setId(id);
        a.setEmail(email);
        return a;
    }
}
