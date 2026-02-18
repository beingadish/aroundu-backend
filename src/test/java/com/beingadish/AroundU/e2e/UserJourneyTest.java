package com.beingadish.AroundU.e2e;

import com.beingadish.AroundU.Constants.Enums.*;
import com.beingadish.AroundU.DTO.Bid.BidCreateRequest;
import com.beingadish.AroundU.DTO.Bid.BidHandshakeRequest;
import com.beingadish.AroundU.DTO.Bid.BidResponseDTO;
import com.beingadish.AroundU.DTO.Job.*;
import com.beingadish.AroundU.DTO.Payment.PaymentLockRequest;
import com.beingadish.AroundU.Entities.*;
import com.beingadish.AroundU.Repository.Address.AddressRepository;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.Skill.SkillRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerRepository;
import com.beingadish.AroundU.Service.BidService;
import com.beingadish.AroundU.Service.JobService;
import com.beingadish.AroundU.Service.PaymentService;
import com.beingadish.AroundU.fixtures.TestFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end user journey test that exercises the complete lifecycle: Client
 * creates job → Worker places bid → Client accepts bid → Worker confirms
 * handshake → Client locks escrow payment → Job transitions through statuses.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("E2E – Full User Journey")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserJourneyTest {

    @Autowired
    private JobService jobService;
    @Autowired
    private BidService bidService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private WorkerRepository workerRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private SkillRepository skillRepository;

    private Client savedClient;
    private Worker savedWorker;
    private Address savedAddress;
    private Skill savedSkill;

    @BeforeEach
    void setup() {
        Address addr = Address.builder()
                .country(Country.US).postalCode("10001").city("New York")
                .area("Manhattan").latitude(40.7128).longitude(-74.006)
                .fullAddress("123 Main St").build();
        savedAddress = addressRepository.save(addr);

        Address workerAddr = Address.builder()
                .country(Country.US).postalCode("10002").city("New York")
                .area("Brooklyn").latitude(40.6782).longitude(-73.9442)
                .fullAddress("456 Worker Ave").build();
        Address savedWorkerAddr = addressRepository.save(workerAddr);

        Client c = Client.builder().build();
        c.setName("E2E Client");
        c.setEmail("e2e-client@test.com");
        c.setPhoneNumber("+10000000005");
        c.setHashedPassword("$2a$10$hashedpassword");
        c.setCurrentAddress(savedAddress);
        c.setCurrency(Currency.USD);
        c.setVerificationStatus(new VerificationStatus(true, null, null, null));
        c.setDeleted(false);
        savedClient = clientRepository.save(c);

        Worker w = Worker.builder().build();
        w.setName("E2E Worker");
        w.setEmail("e2e-worker@test.com");
        w.setPhoneNumber("+10000000006");
        w.setHashedPassword("$2a$10$hashedpassword");
        w.setCurrentAddress(savedWorkerAddr);
        w.setCurrency(Currency.USD);
        w.setVerificationStatus(new VerificationStatus(true, null, null, null));
        w.setDeleted(false);
        w.setIsOnDuty(true);
        w.setOverallRating(4.8);
        w.setExperienceYears(7);
        savedWorker = workerRepository.save(w);

        savedSkill = skillRepository.save(Skill.builder().name("HVAC").build());
    }

    @Test
    @Order(1)
    @DisplayName("Complete user journey: job creation → bid → accept → handshake → payment → status transitions")
    void completeUserJourney() {
        // ── Step 1: Client creates a job ─────────────────────────
        JobCreateRequest jobReq = new JobCreateRequest();
        jobReq.setTitle("Install AC unit");
        jobReq.setShortDescription("New AC in bedroom");
        jobReq.setLongDescription("Install split AC unit in master bedroom, second floor");
        jobReq.setPrice(TestFixtures.priceDTO(800.0));
        jobReq.setJobLocationId(savedAddress.getAddressId());
        jobReq.setJobUrgency(JobUrgency.URGENT);
        jobReq.setRequiredSkillIds(List.of(savedSkill.getId()));
        jobReq.setPaymentMode(PaymentMode.ESCROW);

        JobDetailDTO job = jobService.createJob(savedClient.getId(), jobReq);
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.OPEN_FOR_BIDS);
        assertThat(job.getTitle()).isEqualTo("Install AC unit");

        // Verify client can see their job
        JobDetailDTO clientView = jobService.getJobForClient(job.getId(), savedClient.getId());
        assertThat(clientView.getId()).isEqualTo(job.getId());

        // ── Step 2: Worker places a bid ──────────────────────────
        BidCreateRequest bidReq = new BidCreateRequest();
        bidReq.setBidAmount(750.0);
        bidReq.setNotes("I have 7 years of HVAC experience");

        BidResponseDTO bid = bidService.placeBid(job.getId(), savedWorker.getId(), bidReq);
        assertThat(bid.getStatus()).isEqualTo(BidStatus.PENDING);
        assertThat(bid.getBidAmount()).isEqualTo(750.0);

        // Client can list bids on their job
        List<BidResponseDTO> bids = bidService.listBidsForJob(job.getId());
        assertThat(bids).hasSize(1);

        // ── Step 3: Client accepts the bid ───────────────────────
        BidResponseDTO accepted = bidService.acceptBid(bid.getId(), savedClient.getId());
        assertThat(accepted.getStatus()).isEqualTo(BidStatus.SELECTED);

        // ── Step 4: Worker confirms handshake ────────────────────
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        BidResponseDTO handshook = bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);
        // After worker accepts handshake, bid stays SELECTED
        assertThat(handshook.getStatus()).isEqualTo(BidStatus.SELECTED);

        // ── Step 5: Client locks escrow payment ──────────────────
        PaymentLockRequest lockReq = new PaymentLockRequest();
        lockReq.setAmount(750.0);
        PaymentTransaction payment = paymentService.lockEscrow(job.getId(), savedClient.getId(), lockReq);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ESCROW_LOCKED);

        // ── Step 6: Verify final job state ───────────────────────
        JobDetailDTO finalJob = jobService.getJobDetail(job.getId());
        // After handshake, job should be READY_TO_START
        assertThat(finalJob.getJobStatus()).isEqualTo(JobStatus.READY_TO_START);
    }

    @Test
    @Order(2)
    @DisplayName("Worker rejects handshake – bid returns to non-accepted state")
    void workerRejectsHandshake() {
        // Create job and bid
        JobCreateRequest jobReq = new JobCreateRequest();
        jobReq.setTitle("Fix heating");
        jobReq.setShortDescription("Heater broken");
        jobReq.setLongDescription("Central heating not working");
        jobReq.setPrice(TestFixtures.priceDTO(500.0));
        jobReq.setJobLocationId(savedAddress.getAddressId());
        jobReq.setJobUrgency(JobUrgency.NORMAL);
        jobReq.setRequiredSkillIds(List.of(savedSkill.getId()));
        jobReq.setPaymentMode(PaymentMode.ESCROW);
        JobDetailDTO job = jobService.createJob(savedClient.getId(), jobReq);

        BidCreateRequest bidReq = new BidCreateRequest();
        bidReq.setBidAmount(400.0);
        BidResponseDTO bid = bidService.placeBid(job.getId(), savedWorker.getId(), bidReq);
        bidService.acceptBid(bid.getId(), savedClient.getId());

        // Worker rejects
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(false);
        BidResponseDTO rejected = bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        assertThat(rejected.getStatus()).isEqualTo(BidStatus.REJECTED);
    }

    @Test
    @Order(3)
    @DisplayName("Client cancels job after creation")
    void cancelJob() {
        JobCreateRequest jobReq = new JobCreateRequest();
        jobReq.setTitle("Paint walls");
        jobReq.setShortDescription("Living room");
        jobReq.setLongDescription("Paint living room walls and ceiling");
        jobReq.setPrice(TestFixtures.priceDTO(300.0));
        jobReq.setJobLocationId(savedAddress.getAddressId());
        jobReq.setJobUrgency(JobUrgency.NORMAL);
        jobReq.setRequiredSkillIds(List.of(savedSkill.getId()));
        jobReq.setPaymentMode(PaymentMode.ESCROW);
        JobDetailDTO job = jobService.createJob(savedClient.getId(), jobReq);

        JobStatusUpdateRequest cancelReq = new JobStatusUpdateRequest();
        cancelReq.setNewStatus(JobStatus.CANCELLED);
        JobDetailDTO cancelled = jobService.updateJobStatus(job.getId(), savedClient.getId(), cancelReq);

        assertThat(cancelled.getJobStatus()).isEqualTo(JobStatus.CANCELLED);
    }
}
