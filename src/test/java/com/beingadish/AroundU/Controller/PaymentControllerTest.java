package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import com.beingadish.AroundU.common.constants.enums.PaymentMode;
import com.beingadish.AroundU.common.constants.enums.PaymentStatus;
import com.beingadish.AroundU.payment.controller.PaymentController;
import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.dto.PaymentReleaseRequest;
import com.beingadish.AroundU.payment.dto.PaymentResponseDTO;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import com.beingadish.AroundU.payment.mapper.PaymentTransactionMapper;
import com.beingadish.AroundU.payment.service.PaymentService;
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
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
    + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
    + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
    "spring.data.jpa.repositories.enabled=false"
})
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private PaymentTransactionMapper paymentTransactionMapper;

    // ── JPA/Security infrastructure mocks ────────────────────────
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
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.infrastructure.security.JwtAuthenticationFilter jwtAuthenticationFilter;
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
    private com.beingadish.AroundU.bid.repository.BidRepository bidRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.job.repository.JobRepository jobRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.job.repository.JobConfirmationCodeRepository jobConfirmationCodeRepository;
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

    private PaymentResponseDTO sampleResponseDTO(PaymentStatus status) {
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setId(1L);
        dto.setAmount(500.0);
        dto.setPaymentMode(PaymentMode.ESCROW);
        dto.setStatus(status);
        return dto;
    }

    // ── Lock Escrow ──────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/jobs/{jobId}/payments/lock")
    class LockEscrow {

        @Test
        @DisplayName("201 Created – escrow locked")
        void lock_Success() throws Exception {
            PaymentTransaction tx = sampleTransaction(PaymentStatus.ESCROW_LOCKED);
            when(paymentService.lockEscrow(eq(100L), eq(1L), any(PaymentLockRequest.class)))
                    .thenReturn(tx);
            when(paymentTransactionMapper.toDto(tx)).thenReturn(sampleResponseDTO(PaymentStatus.ESCROW_LOCKED));

            mockMvc.perform(post(JOBS_BASE + "/100/payments/lock")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TestFixtures.paymentLockRequest(500.0))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("ESCROW_LOCKED"));
        }

        @Test
        @DisplayName("404 – job not found")
        void lock_JobNotFound() throws Exception {
            when(paymentService.lockEscrow(eq(999L), eq(1L), any(PaymentLockRequest.class)))
                    .thenThrow(new EntityNotFoundException("Job not found"));

            mockMvc.perform(post(JOBS_BASE + "/999/payments/lock")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TestFixtures.paymentLockRequest(500.0))))
                    .andExpect(status().isNotFound());
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
            PaymentTransaction tx = sampleTransaction(PaymentStatus.RELEASED);
            when(paymentService.releaseEscrow(eq(100L), eq(1L), any(PaymentReleaseRequest.class)))
                    .thenReturn(tx);
            when(paymentTransactionMapper.toDto(tx)).thenReturn(sampleResponseDTO(PaymentStatus.RELEASED));

            mockMvc.perform(post(JOBS_BASE + "/100/payments/release")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TestFixtures.paymentReleaseRequest("RELEASE456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RELEASED"));
        }

        @Test
        @DisplayName("404 – job not found")
        void release_JobNotFound() throws Exception {
            when(paymentService.releaseEscrow(eq(999L), eq(1L), any(PaymentReleaseRequest.class)))
                    .thenThrow(new EntityNotFoundException("Job not found"));

            mockMvc.perform(post(JOBS_BASE + "/999/payments/release")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TestFixtures.paymentReleaseRequest("RELEASE456"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("409 – invalid state")
        void release_InvalidState() throws Exception {
            when(paymentService.releaseEscrow(eq(100L), eq(1L), any(PaymentReleaseRequest.class)))
                    .thenThrow(new IllegalStateException("Payment already released"));

            mockMvc.perform(post(JOBS_BASE + "/100/payments/release")
                    .param("clientId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TestFixtures.paymentReleaseRequest("RELEASE456"))))
                    .andExpect(status().isConflict());
        }
    }
}
