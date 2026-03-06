package com.beingadish.AroundU.Controller;

import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import com.beingadish.AroundU.bid.controller.BidController;
import com.beingadish.AroundU.bid.dto.BidCreateRequest;
import com.beingadish.AroundU.bid.dto.BidHandshakeRequest;
import com.beingadish.AroundU.bid.dto.BidResponseDTO;
import com.beingadish.AroundU.bid.service.BidService;
import com.beingadish.AroundU.common.constants.enums.BidStatus;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BidController.class, excludeAutoConfiguration = {
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
@DisplayName("BidController")
class BidControllerTest {

    private static final String BASE = "/api/v1/bid";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private BidService bidService;
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

    private BidResponseDTO sampleResponse() {
        BidResponseDTO dto = new BidResponseDTO();
        dto.setId(200L);
        dto.setJobId(100L);
        dto.setWorkerId(10L);
        dto.setBidAmount(450.0);
        dto.setStatus(BidStatus.PENDING);
        return dto;
    }

    // ── Place Bid ────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/bid/jobs/{jobId}/bids")
    class PlaceBid {

        @Test
        @DisplayName("201 Created – valid bid")
        void placeBid_Success() throws Exception {
            when(bidService.placeBid(eq(100L), eq(10L), any(BidCreateRequest.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(post(BASE + "/jobs/100/bids")
                            .param("workerId", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TestFixtures.bidCreateRequest(450.0))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(200))
                    .andExpect(jsonPath("$.bidAmount").value(450.0));
        }

        @Test
        @DisplayName("400 Bad Request – missing amount")
        void placeBid_MissingAmount() throws Exception {
            BidCreateRequest req = new BidCreateRequest();
            // bidAmount is null

            mockMvc.perform(post(BASE + "/jobs/100/bids")
                            .param("workerId", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 – job not found")
        void placeBid_JobNotFound() throws Exception {
            when(bidService.placeBid(eq(999L), eq(10L), any(BidCreateRequest.class)))
                    .thenThrow(new EntityNotFoundException("Job not found"));

            mockMvc.perform(post(BASE + "/jobs/999/bids")
                            .param("workerId", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TestFixtures.bidCreateRequest(450.0))))
                    .andExpect(status().isNotFound());
        }
    }

    // ── List Bids ────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/bid/jobs/{jobId}/bids")
    class ListBids {

        @Test
        @DisplayName("200 OK – returns bids for job")
        void listBids_Success() throws Exception {
            when(bidService.listBidsForJob(100L)).thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get(BASE + "/jobs/100/bids"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(200));
        }
    }

    // ── Accept Bid ───────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/bid/bids/{bidId}/accept")
    class AcceptBid {

        @Test
        @DisplayName("200 OK – bid accepted")
        void acceptBid_Success() throws Exception {
            BidResponseDTO resp = sampleResponse();
            resp.setStatus(BidStatus.SELECTED);
            when(bidService.acceptBid(200L, 1L)).thenReturn(resp);

            mockMvc.perform(post(BASE + "/bids/200/accept").param("clientId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SELECTED"));
        }

        @Test
        @DisplayName("404 – bid not found")
        void acceptBid_NotFound() throws Exception {
            when(bidService.acceptBid(999L, 1L))
                    .thenThrow(new EntityNotFoundException("Bid not found"));

            mockMvc.perform(post(BASE + "/bids/999/accept").param("clientId", "1"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── Handshake ────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/bid/bids/{bidId}/handshake")
    class Handshake {

        @Test
        @DisplayName("200 OK – handshake accepted")
        void handshake_Success() throws Exception {
            when(bidService.handshake(eq(200L), eq(10L), any(BidHandshakeRequest.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(post(BASE + "/bids/200/handshake")
                            .param("workerId", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TestFixtures.handshakeRequest(true))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(200));
        }

        @Test
        @DisplayName("400 – missing accepted field")
        void handshake_MissingField() throws Exception {
            BidHandshakeRequest req = new BidHandshakeRequest();
            // accepted is null

            mockMvc.perform(post(BASE + "/bids/200/handshake")
                            .param("workerId", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }
}
