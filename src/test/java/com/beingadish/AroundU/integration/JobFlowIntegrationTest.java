package com.beingadish.AroundU.integration;

import com.beingadish.AroundU.common.constants.enums.*;
import com.beingadish.AroundU.job.dto.*;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.common.entity.VerificationStatus;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.common.entity.Skill;
import com.beingadish.AroundU.location.repository.AddressRepository;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.common.repository.SkillRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.job.service.JobService;
import com.beingadish.AroundU.fixtures.TestFixtures;
import com.beingadish.AroundU.common.util.PageResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Job creation → update → status-change flow. Uses
 * real service + repository layers backed by an in-memory H2 database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration – Job Flow")
class JobFlowIntegrationTest {

    @Autowired
    private JobService jobService;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private WorkerRepository workerRepository;

    private Client savedClient;
    private Address savedAddress;
    private Skill savedSkill;

    @BeforeEach
    void seedData() {
        Address addr = Address.builder()
                .country(Country.US).postalCode("10001").city("New York")
                .area("Manhattan").latitude(40.7128).longitude(-74.006)
                .fullAddress("123 Main St").build();
        savedAddress = addressRepository.save(addr);

        Client c = Client.builder().build();
        c.setName("Integration Client");
        c.setEmail("integ-client@test.com");
        c.setPhoneNumber("+10000000000");
        c.setHashedPassword("$2a$10$hashedpassword");
        c.setCurrentAddress(savedAddress);
        c.setCurrency(Currency.USD);
        c.setVerificationStatus(new VerificationStatus(true, null, null, null));
        c.setDeleted(false);
        savedClient = clientRepository.save(c);

        savedSkill = skillRepository.save(Skill.builder().name("Plumbing").build());
    }

    // ── Create ───────────────────────────────────────────────────
    @Test
    @DisplayName("Create job persists in DB with OPEN_FOR_BIDS status")
    void createJob_PersistsWithOpenStatus() {
        JobCreateRequest req = new JobCreateRequest();
        req.setTitle("Fix sink");
        req.setShortDescription("Leaking");
        req.setLongDescription("Kitchen sink leaks");
        req.setPrice(TestFixtures.priceDTO(300.0));
        req.setJobLocationId(savedAddress.getAddressId());
        req.setJobUrgency(JobUrgency.NORMAL);
        req.setRequiredSkillIds(List.of(savedSkill.getId()));
        req.setPaymentMode(PaymentMode.ESCROW);

        JobDetailDTO result = jobService.createJob(savedClient.getId(), req);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Fix sink");
        assertThat(result.getJobStatus()).isEqualTo(JobStatus.OPEN_FOR_BIDS);
        assertThat(jobRepository.findById(result.getId())).isPresent();
    }

    // ── Update ───────────────────────────────────────────────────
    @Test
    @DisplayName("Update job title changes the persisted entity")
    void updateJob_ChangesTitleInDB() {
        JobDetailDTO created = createSampleJob();

        JobUpdateRequest update = new JobUpdateRequest();
        update.setTitle("Fix sink - updated");
        JobDetailDTO updated = jobService.updateJob(created.getId(), savedClient.getId(), update);

        assertThat(updated.getTitle()).isEqualTo("Fix sink - updated");
    }

    // ── Delete ───────────────────────────────────────────────────
    @Test
    @DisplayName("Delete job removes the entity from repository")
    void deleteJob_RemovesFromDB() {
        JobDetailDTO created = createSampleJob();
        jobService.deleteJob(created.getId(), savedClient.getId());

        assertThat(jobRepository.findById(created.getId())).isEmpty();
    }

    // ── Get Detail ───────────────────────────────────────────────
    @Test
    @DisplayName("Get job detail returns correct data")
    void getJobDetail_ReturnsCorrectData() {
        JobDetailDTO created = createSampleJob();
        JobDetailDTO detail = jobService.getJobDetail(created.getId());

        assertThat(detail.getId()).isEqualTo(created.getId());
        assertThat(detail.getTitle()).isEqualTo("Fix sink");
    }

    // ── Client Jobs ──────────────────────────────────────────────
    @Test
    @DisplayName("Get client jobs returns paginated results")
    void clientJobs_ReturnsPaginated() {
        createSampleJob();
        createSampleJob();

        JobFilterRequest filter = new JobFilterRequest();
        filter.setPage(0);
        filter.setSize(10);
        PageResponse<JobSummaryDTO> page = jobService.getClientJobs(savedClient.getId(), filter);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    // ── Status Update ─────────────────────────────────────────────
    @Test
    @DisplayName("Update job status transitions from OPEN to CANCELLED")
    void updateStatus_TransitionsToCancelled() {
        JobDetailDTO created = createSampleJob();

        JobStatusUpdateRequest statusReq = new JobStatusUpdateRequest();
        statusReq.setNewStatus(JobStatus.CANCELLED);
        JobDetailDTO updated = jobService.updateJobStatus(created.getId(), savedClient.getId(), statusReq);

        assertThat(updated.getJobStatus()).isEqualTo(JobStatus.CANCELLED);
    }

    // ── helper ───────────────────────────────────────────────────
    private JobDetailDTO createSampleJob() {
        JobCreateRequest req = new JobCreateRequest();
        req.setTitle("Fix sink");
        req.setShortDescription("Leaking");
        req.setLongDescription("Kitchen sink leaks");
        req.setPrice(TestFixtures.priceDTO(300.0));
        req.setJobLocationId(savedAddress.getAddressId());
        req.setJobUrgency(JobUrgency.NORMAL);
        req.setRequiredSkillIds(List.of(savedSkill.getId()));
        req.setPaymentMode(PaymentMode.ESCROW);
        return jobService.createJob(savedClient.getId(), req);
    }
}
