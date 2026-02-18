package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import com.beingadish.AroundU.Constants.Enums.PaymentMode;
import com.beingadish.AroundU.Constants.Enums.PaymentStatus;
import com.beingadish.AroundU.Controller.Payment.PaymentController;
import com.beingadish.AroundU.DTO.Payment.PaymentLockRequest;
import com.beingadish.AroundU.DTO.Payment.PaymentReleaseRequest;
import com.beingadish.AroundU.Entities.PaymentTransaction;
import com.beingadish.AroundU.Service.PaymentService;
import com.beingadish.AroundU.fixtures.TestFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = PaymentController.class, excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(TestWebSecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "spring.data.jpa.repositories.enabled=false"
})
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private PaymentService paymentService;

    // ── JPA/Security infrastructure mocks ────────────────────────
    @SuppressWarnings("unused") @MockitoBean(name = "entityManagerFactory") private EntityManagerFactory entityManagerFactory;
    @SuppressWarnings("unused") @MockitoBean(name = "jpaSharedEM_entityManagerFactory") private jakarta.persistence.EntityManager sharedEntityManager;
    @SuppressWarnings("unused") @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @SuppressWarnings("unused") @MockitoBean private PlatformTransactionManager platformTransactionManager;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Security.JwtAuthenticationFilter jwtAuthenticationFilter;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Client.ClientReadRepository clientReadRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Client.ClientWriteRepository clientWriteRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Client.ClientRepository clientRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Worker.WorkerReadRepository workerReadRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Worker.WorkerWriteRepository workerWriteRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Worker.WorkerRepository workerRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Admin.AdminRepository adminRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Skill.SkillRepository skillRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Bid.BidRepository bidRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Job.JobRepository jobRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Job.JobConfirmationCodeRepository jobConfirmationCodeRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Payment.PaymentTransactionRepository paymentTransactionRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Address.AddressRepository addressRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.FailedGeoSync.FailedGeoSyncRepository failedGeoSyncRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Analytics.AggregatedMetricsRepository aggregatedMetricsRepository;
    @SuppressWarnings("unused") @MockitoBean private com.beingadish.AroundU.Repository.Notification.FailedNotificationRepository failedNotificationRepository;

    @BeforeEach
    void stubEntityManager() {
        when(sharedEntityManager.getDelegate()).thenReturn(new Object());
        when(entityManagerFactory.createEntityManager()).thenReturn(sharedEntityManager);
    }

    private static final String JOBS_BASE = "/api/v1/jobs";

    private PaymentTransaction sampleTransaction(PaymentStatus status) {
        return PaymentTransaction.builder()
                .id(1L)
                .amount(500.0)
                .paymentMode(PaymentMode.ESCROW)
                .status(status)
                .build();
    }

    // ── Lock Escrow ──────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/jobs/{jobId}/payments/lock")
    class LockEscrow {

        @Test
        @DisplayName("201 Created – escrow locked")
        void lock_Success() throws Exception {
            when(paymentService.lockEscrow(eq(100L), eq(1L), any(PaymentLockRequest.class)))
                    .thenReturn(sampleTransaction(PaymentStatus.ESCROW_LOCKED));

            mockMvc.perform(post(JOBS_BASE + "/100/payments/lock")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TestFixtures.paymentLockRequest(500.0))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("ESCROW_LOCKED"));
        }

        @Test
        @DisplayName("500 – job not found (EntityNotFoundException falls through)")
        void lock_JobNotFound() throws Exception {
            when(paymentService.lockEscrow(eq(999L), eq(1L), any(PaymentLockRequest.class)))
                    .thenThrow(new EntityNotFoundException("Job not found"));

            mockMvc.perform(post(JOBS_BASE + "/999/payments/lock")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TestFixtures.paymentLockRequest(500.0))))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("400 – missing amount")
        void lock_MissingAmount() throws Exception {
            PaymentLockRequest req = new PaymentLockRequest();
            // amount is null

            mockMvc.perform(post(JOBS_BASE + "/100/payments/lock")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── Release Escrow ───────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/jobs/{jobId}/payments/release")
    class ReleaseEscrow {

        @Test
        @DisplayName("200 OK – escrow released")
        void release_Success() throws Exception {
            when(paymentService.releaseEscrow(eq(100L), eq(1L), any(PaymentReleaseRequest.class)))
                    .thenReturn(sampleTransaction(PaymentStatus.RELEASED));

            mockMvc.perform(post(JOBS_BASE + "/100/payments/release")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TestFixtures.paymentReleaseRequest("RELEASE456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RELEASED"));
        }

        @Test
        @DisplayName("500 – job not found (EntityNotFoundException falls through)")
        void release_JobNotFound() throws Exception {
            when(paymentService.releaseEscrow(eq(999L), eq(1L), any(PaymentReleaseRequest.class)))
                    .thenThrow(new EntityNotFoundException("Job not found"));

            mockMvc.perform(post(JOBS_BASE + "/999/payments/release")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TestFixtures.paymentReleaseRequest("RELEASE456"))))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("500 – invalid state throws (IllegalStateException falls through)")
        void release_InvalidState() throws Exception {
            when(paymentService.releaseEscrow(eq(100L), eq(1L), any(PaymentReleaseRequest.class)))
                    .thenThrow(new IllegalStateException("Payment already released"));

            mockMvc.perform(post(JOBS_BASE + "/100/payments/release")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TestFixtures.paymentReleaseRequest("RELEASE456"))))
                    .andExpect(status().isInternalServerError());
        }
    }
}
