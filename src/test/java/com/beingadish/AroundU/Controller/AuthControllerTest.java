package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import com.beingadish.AroundU.user.controller.AuthController;
import com.beingadish.AroundU.user.dto.auth.LoginRequestDTO;
import com.beingadish.AroundU.user.dto.auth.LoginResponseDTO;
import com.beingadish.AroundU.user.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private static final String LOGIN_URL = "/api/v1/auth/login";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AuthService authService;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.infrastructure.security.JwtAuthenticationFilter jwtAuthenticationFilter;
    // ── JPA infrastructure mocks (no DB in WebMvcTest slice) ────
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.user.repository.ClientReadRepository clientReadRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.user.repository.ClientWriteRepository clientWriteRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.user.repository.ClientRepository clientRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.user.repository.WorkerReadRepository workerReadRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.user.repository.WorkerWriteRepository workerWriteRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.user.repository.WorkerRepository workerRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.user.repository.AdminRepository adminRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.common.repository.SkillRepository skillRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.job.repository.JobRepository jobRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.job.repository.JobConfirmationCodeRepository jobConfirmationCodeRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.bid.repository.BidRepository bidRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.payment.repository.PaymentTransactionRepository paymentTransactionRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.location.repository.AddressRepository addressRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.location.repository.FailedGeoSyncRepository failedGeoSyncRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.infrastructure.analytics.repository.AggregatedMetricsRepository aggregatedMetricsRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.notification.repository.FailedNotificationRepository failedNotificationRepository;
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

    private LoginRequestDTO loginRequest(String email, String password) {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    // ── Successful Login ─────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/auth/login – success")
    class LoginSuccess {

        @Test
        @DisplayName("200 OK – client login returns JWT")
        void clientLogin() throws Exception {
            LoginRequestDTO req = loginRequest("client@test.com", "password");
            when(authService.authenticate(any(LoginRequestDTO.class)))
                    .thenReturn(new LoginResponseDTO(1L, "jwt-client", "Bearer", "client@test.com", "ROLE_CLIENT"));

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
            when(authService.authenticate(any(LoginRequestDTO.class)))
                    .thenReturn(new LoginResponseDTO(10L, "jwt-worker", "Bearer", "worker@test.com", "ROLE_WORKER"));

            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(10))
                    .andExpect(jsonPath("$.role").value("ROLE_WORKER"));
        }

        @Test
        @DisplayName("200 OK – admin login returns JWT")
        void adminLogin() throws Exception {
            LoginRequestDTO req = loginRequest("admin@test.com", "password");
            when(authService.authenticate(any(LoginRequestDTO.class)))
                    .thenReturn(new LoginResponseDTO(99L, "jwt-admin", "Bearer", "admin@test.com", "ROLE_ADMIN"));

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
            when(authService.authenticate(any(LoginRequestDTO.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 – user not found during resolution")
        void userNotFound() throws Exception {
            LoginRequestDTO req = loginRequest("unknown@test.com", "password");
            when(authService.authenticate(any(LoginRequestDTO.class)))
                    .thenThrow(new EntityNotFoundException("User not found"));

            mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }
}
