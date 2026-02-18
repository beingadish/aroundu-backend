package com.beingadish.AroundU.integration;

import com.beingadish.AroundU.Constants.Enums.*;
import com.beingadish.AroundU.DTO.Bid.BidCreateRequest;
import com.beingadish.AroundU.DTO.Bid.BidHandshakeRequest;
import com.beingadish.AroundU.DTO.Bid.BidResponseDTO;
import com.beingadish.AroundU.DTO.Job.JobCreateRequest;
import com.beingadish.AroundU.DTO.Job.JobDetailDTO;
import com.beingadish.AroundU.Entities.*;
import com.beingadish.AroundU.Repository.Address.AddressRepository;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.Skill.SkillRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerRepository;
import com.beingadish.AroundU.Service.BidService;
import com.beingadish.AroundU.Service.JobService;
import com.beingadish.AroundU.fixtures.TestFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Bid placement → accept → handshake flow.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration – Bid Flow")
class BidFlowIntegrationTest {

    @Autowired
    private JobService jobService;
    @Autowired
    private BidService bidService;
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
    void seedData() {
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
        c.setName("Bid Test Client");
        c.setEmail("bid-client@test.com");
        c.setPhoneNumber("+10000000001");
        c.setHashedPassword("$2a$10$hashedpassword");
        c.setCurrentAddress(savedAddress);
        c.setCurrency(Currency.USD);
        c.setVerificationStatus(new VerificationStatus(true, null, null, null));
        c.setDeleted(false);
        savedClient = clientRepository.save(c);

        Worker w = Worker.builder().build();
        w.setName("Bid Test Worker");
        w.setEmail("bid-worker@test.com");
        w.setPhoneNumber("+10000000002");
        w.setHashedPassword("$2a$10$hashedpassword");
        w.setCurrentAddress(savedWorkerAddr);
        w.setCurrency(Currency.USD);
        w.setVerificationStatus(new VerificationStatus(true, null, null, null));
        w.setDeleted(false);
        w.setIsOnDuty(true);
        w.setOverallRating(4.0);
        w.setExperienceYears(3);
        savedWorker = workerRepository.save(w);

        savedSkill = skillRepository.save(Skill.builder().name("Electrical").build());
    }

    // ── Place Bid ────────────────────────────────────────────────
    @Test
    @DisplayName("Place bid on open job creates PENDING bid")
    void placeBid_CreatesPendingBid() {
        Long jobId = createOpenJob();

        BidCreateRequest req = new BidCreateRequest();
        req.setBidAmount(250.0);
        req.setNotes("Available now");

        BidResponseDTO result = bidService.placeBid(jobId, savedWorker.getId(), req);

        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(jobId);
        assertThat(result.getStatus()).isEqualTo(BidStatus.PENDING);
        assertThat(result.getBidAmount()).isEqualTo(250.0);
    }

    // ── List Bids ────────────────────────────────────────────────
    @Test
    @DisplayName("List bids returns all bids for the job")
    void listBids_ReturnsAll() {
        Long jobId = createOpenJob();

        BidCreateRequest req = new BidCreateRequest();
        req.setBidAmount(250.0);
        bidService.placeBid(jobId, savedWorker.getId(), req);

        List<BidResponseDTO> bids = bidService.listBidsForJob(jobId);
        assertThat(bids).hasSize(1);
    }

    // ── Accept Bid ───────────────────────────────────────────────
    @Test
    @DisplayName("Accept bid changes status to SELECTED")
    void acceptBid_SetsStatusToSelected() {
        Long jobId = createOpenJob();

        BidCreateRequest req = new BidCreateRequest();
        req.setBidAmount(250.0);
        BidResponseDTO placed = bidService.placeBid(jobId, savedWorker.getId(), req);

        BidResponseDTO accepted = bidService.acceptBid(placed.getId(), savedClient.getId());

        assertThat(accepted.getStatus()).isEqualTo(BidStatus.SELECTED);
    }

    // ── Full Flow: Place → Accept → Handshake ────────────────────
    @Test
    @DisplayName("Full bid lifecycle: place → accept → worker handshake")
    void fullBidLifecycle() {
        Long jobId = createOpenJob();

        // Place
        BidCreateRequest bidReq = new BidCreateRequest();
        bidReq.setBidAmount(250.0);
        BidResponseDTO placed = bidService.placeBid(jobId, savedWorker.getId(), bidReq);
        assertThat(placed.getStatus()).isEqualTo(BidStatus.PENDING);

        // Accept
        BidResponseDTO accepted = bidService.acceptBid(placed.getId(), savedClient.getId());
        assertThat(accepted.getStatus()).isEqualTo(BidStatus.SELECTED);

        // Handshake
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        BidResponseDTO handshook = bidService.handshake(placed.getId(), savedWorker.getId(), hsReq);

        // After worker accepts handshake, bid stays SELECTED
        assertThat(handshook.getStatus()).isEqualTo(BidStatus.SELECTED);
    }

    // ── helper ───────────────────────────────────────────────────
    private Long createOpenJob() {
        JobCreateRequest req = new JobCreateRequest();
        req.setTitle("Electrical work");
        req.setShortDescription("Fix wiring");
        req.setLongDescription("Rewire kitchen");
        req.setPrice(TestFixtures.priceDTO(400.0));
        req.setJobLocationId(savedAddress.getAddressId());
        req.setJobUrgency(JobUrgency.NORMAL);
        req.setRequiredSkillIds(List.of(savedSkill.getId()));
        req.setPaymentMode(PaymentMode.ESCROW);
        JobDetailDTO created = jobService.createJob(savedClient.getId(), req);
        return created.getId();
    }
}
