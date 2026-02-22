package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import com.beingadish.AroundU.common.constants.enums.Currency;
import com.beingadish.AroundU.common.constants.enums.JobUrgency;
import com.beingadish.AroundU.common.constants.enums.PaymentMode;
import com.beingadish.AroundU.common.dto.PriceDTO;
import com.beingadish.AroundU.common.util.PageResponse;
import com.beingadish.AroundU.fixtures.TestFixtures;
import com.beingadish.AroundU.infrastructure.security.UserPrincipal;
import com.beingadish.AroundU.job.controller.JobController;
import com.beingadish.AroundU.job.dto.*;
import com.beingadish.AroundU.job.exception.JobNotFoundException;
import com.beingadish.AroundU.job.exception.JobValidationException;
import com.beingadish.AroundU.job.service.JobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = JobController.class, excludeAutoConfiguration = {
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
@DisplayName("JobController")
class JobControllerTest {

    private static final String BASE = "/api/v1/jobs";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JobService jobService;
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

    private RequestPostProcessor authenticatedUser(Long id, String role) {
        UserPrincipal principal = UserPrincipal.builder()
                .id(id)
                .email("user@example.com")
                .password("pw")
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.setUserPrincipal(authentication);
            return SecurityMockMvcRequestPostProcessors.authentication(authentication).postProcessRequest(request);
        };
    }

    // ── Create Job ───────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/jobs")
    class CreateJob {

        @Test
        @DisplayName("201 Created – valid request")
        void createJob_Success() throws Exception {
            JobDetailDTO dto = TestFixtures.jobDetailDTO();
            when(jobService.createJob(eq(1L), any(JobCreateRequest.class))).thenReturn(dto);

            mockMvc.perform(post(BASE)
                            .param("clientId", "1")
                            .with(authenticatedUser(1L, "ROLE_CLIENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TestFixtures.jobCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100));
        }

        @Test
        @DisplayName("400 Bad Request – missing title")
        void createJob_MissingTitle() throws Exception {
            JobCreateRequest request = new JobCreateRequest();
            request.setLongDescription("desc");
            request.setPrice(PriceDTO.builder().currency(Currency.USD).amount(100.0).build());
            request.setJobLocationId(1L);
            request.setJobUrgency(JobUrgency.NORMAL);
            request.setRequiredSkillIds(List.of(1L));
            request.setPaymentMode(PaymentMode.ESCROW);
            // title is missing

            mockMvc.perform(post(BASE)
                            .param("clientId", "1")
                            .with(authenticatedUser(1L, "ROLE_CLIENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request – service validation fails")
        void createJob_ValidationError() throws Exception {
            when(jobService.createJob(eq(1L), any(JobCreateRequest.class)))
                    .thenThrow(new JobValidationException("Client not found"));

            mockMvc.perform(post(BASE)
                            .param("clientId", "1")
                            .with(authenticatedUser(1L, "ROLE_CLIENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TestFixtures.jobCreateRequest())))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── Get Job ──────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/jobs/{jobId}")
    class GetJob {

        @Test
        @DisplayName("200 OK – job found")
        void getJob_Success() throws Exception {
            when(jobService.getJobDetail(100L)).thenReturn(TestFixtures.jobDetailDTO());

            mockMvc.perform(get(BASE + "/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(100))
                    .andExpect(jsonPath("$.data.title").value("Fix plumbing"));
        }

        @Test
        @DisplayName("404 Not Found – job does not exist")
        void getJob_NotFound() throws Exception {
            when(jobService.getJobDetail(999L)).thenThrow(new JobNotFoundException("Job not found"));

            mockMvc.perform(get(BASE + "/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── List Jobs ────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/jobs")
    class ListJobs {

        @Test
        @DisplayName("200 OK – search by city")
        void listJobs_Success() throws Exception {
            JobSummaryDTO summary = new JobSummaryDTO();
            summary.setId(100L);
            summary.setTitle("Fix plumbing");
            when(jobService.listJobs(eq("New York"), isNull(), isNull()))
                    .thenReturn(List.of(summary));

            mockMvc.perform(get(BASE).param("city", "New York"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(100));
        }
    }

    // ── Delete Job ───────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/v1/jobs/{jobId}")
    class DeleteJob {

        @Test
        @DisplayName("200 OK – soft delete")
        void deleteJob_Success() throws Exception {
            doNothing().when(jobService).deleteJob(100L, 1L);

            mockMvc.perform(delete(BASE + "/100").param("clientId", "1")
                            .with(authenticatedUser(1L, "ROLE_CLIENT")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Job deleted successfully"));
        }

        @Test
        @DisplayName("404 – job not found")
        void deleteJob_NotFound() throws Exception {
            doThrow(new JobNotFoundException("Job not found")).when(jobService).deleteJob(999L, 1L);

            mockMvc.perform(delete(BASE + "/999").param("clientId", "1")
                            .with(authenticatedUser(1L, "ROLE_CLIENT")))
                    .andExpect(status().isNotFound());
        }
    }

    // ── Worker Feed ──────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/jobs/worker/{workerId}/feed")
    class WorkerFeed {

        @Test
        @DisplayName("200 OK – returns feed")
        void workerFeed_Success() throws Exception {
            JobSummaryDTO summary = new JobSummaryDTO();
            summary.setId(100L);
            summary.setTitle("Fix plumbing");
            PageResponse<JobSummaryDTO> page = new PageResponse<>(new PageImpl<>(List.of(summary)));
            when(jobService.getWorkerFeed(eq(10L), any(WorkerJobFeedRequest.class))).thenReturn(page);

            mockMvc.perform(get(BASE + "/worker/10/feed")
                            .with(authenticatedUser(10L, "ROLE_WORKER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(100));
        }
    }

    // ── Client Jobs ──────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/jobs/client/{clientId}")
    class ClientJobs {

        @Test
        @DisplayName("200 OK – returns client's jobs")
        void clientJobs_Success() throws Exception {
            JobSummaryDTO summary = new JobSummaryDTO();
            summary.setId(100L);
            PageResponse<JobSummaryDTO> page = new PageResponse<>(new PageImpl<>(List.of(summary)));
            when(jobService.getClientJobs(eq(1L), any(JobFilterRequest.class))).thenReturn(page);

            mockMvc.perform(get(BASE + "/client/1")
                            .with(authenticatedUser(1L, "ROLE_CLIENT")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(100));
        }
    }

    // ── Update Job Status ────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /api/v1/jobs/{jobId}/status")
    class UpdateStatus {

        @Test
        @DisplayName("200 OK – valid status update")
        void updateStatus_Success() throws Exception {
            when(jobService.updateJobStatus(eq(100L), eq(1L), any(JobStatusUpdateRequest.class)))
                    .thenReturn(TestFixtures.jobDetailDTO());

            mockMvc.perform(patch(BASE + "/100/status")
                            .param("clientId", "1")
                            .with(authenticatedUser(1L, "ROLE_CLIENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newStatus\":\"BID_SELECTED_AWAITING_HANDSHAKE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(100));
        }
    }
}
