package com.beingadish.AroundU.Controller.Auth;

import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import com.beingadish.AroundU.DTO.Auth.LoginRequestDTO;
import com.beingadish.AroundU.DTO.Auth.LoginResponseDTO;
import com.beingadish.AroundU.Service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.EntityManagerFactory;

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
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
    "spring.data.jpa.repositories.enabled=false"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Security.JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── JPA infrastructure mocks ─────────────────────────────────
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Client.ClientReadRepository clientReadRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Client.ClientWriteRepository clientWriteRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Client.ClientRepository clientRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Worker.WorkerReadRepository workerReadRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Worker.WorkerWriteRepository workerWriteRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Worker.WorkerRepository workerRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Admin.AdminRepository adminRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Skill.SkillRepository skillRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Bid.BidRepository bidRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Job.JobRepository jobRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Job.JobConfirmationCodeRepository jobConfirmationCodeRepository;
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

    @BeforeEach
    void stubEntityManager() {
        when(sharedEntityManager.getDelegate()).thenReturn(new Object());
        when(entityManagerFactory.createEntityManager()).thenReturn(sharedEntityManager);
    }

    @Test
    void authenticateClientReturnsTokenAndRole() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("client@example.com");
        request.setPassword("secret");

        when(authService.authenticate(any(LoginRequestDTO.class)))
                .thenReturn(new LoginResponseDTO(7L, "jwt-token", "Bearer", "client@example.com", "ROLE_CLIENT"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.email").value("client@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_CLIENT"));
    }

    @Test
    void authenticateWorkerResolvesWorkerId() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("worker@example.com");
        request.setPassword("secret");

        when(authService.authenticate(any(LoginRequestDTO.class)))
                .thenReturn(new LoginResponseDTO(15L, "jwt-worker-token", "Bearer", "worker@example.com", "ROLE_WORKER"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(15))
                .andExpect(jsonPath("$.token").value("jwt-worker-token"))
                .andExpect(jsonPath("$.role").value("ROLE_WORKER"));
    }

    @Test
    void authenticateAdminResolvesAdminId() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@example.com");
        request.setPassword("secret");

        when(authService.authenticate(any(LoginRequestDTO.class)))
                .thenReturn(new LoginResponseDTO(99L, "jwt-admin-token", "Bearer", "admin@example.com", "ROLE_ADMIN"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(99))
                .andExpect(jsonPath("$.token").value("jwt-admin-token"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }
}
