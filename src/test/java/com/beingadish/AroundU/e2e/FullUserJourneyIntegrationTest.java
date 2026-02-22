package com.beingadish.AroundU.e2e;

import com.beingadish.AroundU.common.constants.enums.*;
import com.beingadish.AroundU.bid.dto.BidCreateRequest;
import com.beingadish.AroundU.bid.dto.BidHandshakeRequest;
import com.beingadish.AroundU.bid.dto.BidResponseDTO;
import com.beingadish.AroundU.chat.entity.Conversation;
import com.beingadish.AroundU.chat.repository.ConversationRepository;
import com.beingadish.AroundU.job.dto.*;

import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import com.beingadish.AroundU.review.dto.ReviewCreateRequest;
import com.beingadish.AroundU.review.dto.ReviewResponseDTO;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.common.entity.VerificationStatus;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.common.entity.Skill;
import com.beingadish.AroundU.location.repository.AddressRepository;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.common.repository.SkillRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.bid.service.BidService;
import com.beingadish.AroundU.job.service.JobService;
import com.beingadish.AroundU.payment.service.PaymentService;
import com.beingadish.AroundU.review.service.ReviewService;
import com.beingadish.AroundU.fixtures.TestFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Full-lifecycle integration tests covering all three user journeys:
 * 1. Client Posts Job → Worker Bids → Accept → Escrow → Start
 * 2. Worker Completes → Client Releases Payment → Reviews
 * 3. Chat auto-created on bid acceptance
 *
 * Also tests escrow guards, invalid transitions, and edge cases.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Full User Journey Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullUserJourneyIntegrationTest {

    @Autowired private JobService jobService;
    @Autowired private BidService bidService;
    @Autowired private PaymentService paymentService;
    @Autowired private ReviewService reviewService;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private SkillRepository skillRepository;

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
        c.setName("Journey Client");
        c.setEmail("journey-client@test.com");
        c.setPhoneNumber("+10000000100");
        c.setHashedPassword("$2a$10$hashedpassword");
        c.setCurrentAddress(savedAddress);
        c.setCurrency(Currency.USD);
        c.setVerificationStatus(new VerificationStatus(true, null, null, null));
        c.setDeleted(false);
        savedClient = clientRepository.save(c);

        Worker w = Worker.builder().build();
        w.setName("Journey Worker");
        w.setEmail("journey-worker@test.com");
        w.setPhoneNumber("+10000000101");
        w.setHashedPassword("$2a$10$hashedpassword");
        w.setCurrentAddress(savedWorkerAddr);
        w.setCurrency(Currency.USD);
        w.setVerificationStatus(new VerificationStatus(true, null, null, null));
        w.setDeleted(false);
        w.setIsOnDuty(true);
        w.setOverallRating(4.5);
        w.setExperienceYears(5);
        savedWorker = workerRepository.save(w);

        savedSkill = skillRepository.save(Skill.builder().name("Plumbing").build());
    }

    // ═══════════════════════════════════════════════════════════════
    // JOURNEY 1: Client Posts Job → Full Accept Offer Flow
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("J1: Complete accept-offer flow with escrow lock")
    void journey1_acceptOfferWithEscrow() {
        // Step 1: Client creates job
        JobDetailDTO job = createTestJob();
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.OPEN_FOR_BIDS);

        // Step 2: Worker places bid
        BidResponseDTO bid = placeBid(job.getId(), 750.0);
        assertThat(bid.getStatus()).isEqualTo(BidStatus.PENDING);

        // Step 3: Client accepts bid
        BidResponseDTO accepted = bidService.acceptBid(bid.getId(), savedClient.getId());
        assertThat(accepted.getStatus()).isEqualTo(BidStatus.SELECTED);

        // Verify conversation was auto-created
        List<Conversation> convos = conversationRepository.findByJobId(job.getId());
        assertThat(convos).hasSize(1);
        assertThat(convos.get(0).getParticipantOneId()).isEqualTo(savedClient.getId());
        assertThat(convos.get(0).getParticipantTwoId()).isEqualTo(savedWorker.getId());

        // Step 4: Worker handshake
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        // Verify job is READY_TO_START
        JobDetailDTO readyJob = jobService.getJobDetail(job.getId());
        assertThat(readyJob.getJobStatus()).isEqualTo(JobStatus.READY_TO_START);

        // Step 5: Client locks escrow
        PaymentLockRequest lockReq = new PaymentLockRequest();
        lockReq.setAmount(750.0);
        PaymentTransaction payment = paymentService.lockEscrow(job.getId(), savedClient.getId(), lockReq);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ESCROW_LOCKED);
        assertThat(payment.getAmount()).isEqualTo(750.0);

        // Step 6: Worker starts task
        JobStatusUpdateRequest startReq = new JobStatusUpdateRequest();
        startReq.setNewStatus(JobStatus.IN_PROGRESS);
        JobDetailDTO inProgress = jobService.updateJobStatusByWorker(job.getId(), savedWorker.getId(), startReq);
        assertThat(inProgress.getJobStatus()).isEqualTo(JobStatus.IN_PROGRESS);
    }

    // ═══════════════════════════════════════════════════════════════
    // JOURNEY 2: Task Completion Flow
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("J2: Worker marks complete → Client releases payment → Both review")
    void journey2_taskCompletionAndReview() {
        // Setup: get to IN_PROGRESS state
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 600.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        PaymentLockRequest lockReq = new PaymentLockRequest();
        lockReq.setAmount(600.0);
        paymentService.lockEscrow(job.getId(), savedClient.getId(), lockReq);

        JobStatusUpdateRequest startReq = new JobStatusUpdateRequest();
        startReq.setNewStatus(JobStatus.IN_PROGRESS);
        jobService.updateJobStatusByWorker(job.getId(), savedWorker.getId(), startReq);

        // Step 1: Worker marks task complete
        JobStatusUpdateRequest completeReq = new JobStatusUpdateRequest();
        completeReq.setNewStatus(JobStatus.COMPLETED_PENDING_PAYMENT);
        JobDetailDTO completedPending = jobService.updateJobStatusByWorker(
                job.getId(), savedWorker.getId(), completeReq);
        assertThat(completedPending.getJobStatus()).isEqualTo(JobStatus.COMPLETED_PENDING_PAYMENT);

        // Step 2: Client transitions job to COMPLETED (via COMPLETED_PENDING_PAYMENT → COMPLETED)
        JobStatusUpdateRequest finalComplete = new JobStatusUpdateRequest();
        finalComplete.setNewStatus(JobStatus.COMPLETED);
        jobService.updateJobStatus(job.getId(), savedClient.getId(), finalComplete);

        // Step 3: Client leaves review
        ReviewCreateRequest clientReview = new ReviewCreateRequest();
        clientReview.setRating(5.0);
        clientReview.setReviewComment("Excellent work!");
        ReviewResponseDTO review = reviewService.submitReview(job.getId(), savedClient.getId(), clientReview);
        assertThat(review.getRating()).isEqualTo(5.0);
        assertThat(review.getJobId()).isEqualTo(job.getId());

        // Step 4: Worker leaves review
        ReviewCreateRequest workerReview = new ReviewCreateRequest();
        workerReview.setRating(4.0);
        workerReview.setReviewComment("Good client, clear instructions");
        ReviewResponseDTO wReview = reviewService.submitWorkerReview(job.getId(), savedWorker.getId(), workerReview);
        assertThat(wReview.getRating()).isEqualTo(4.0);
    }

    // ═══════════════════════════════════════════════════════════════
    // JOURNEY 3: Chat Auto-creation
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("J3: Conversation auto-created when bid accepted")
    void journey3_chatAutoCreation() {
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 500.0);

        // Before acceptance: no conversations
        List<Conversation> before = conversationRepository.findByJobId(job.getId());
        assertThat(before).isEmpty();

        // Accept bid
        bidService.acceptBid(bid.getId(), savedClient.getId());

        // After acceptance: conversation exists
        List<Conversation> after = conversationRepository.findByJobId(job.getId());
        assertThat(after).hasSize(1);
        Conversation convo = after.get(0);
        assertThat(convo.getParticipantOneId()).isEqualTo(savedClient.getId());
        assertThat(convo.getParticipantTwoId()).isEqualTo(savedWorker.getId());
    }

    // ═══════════════════════════════════════════════════════════════
    // ESCROW GUARD TESTS
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("Escrow: Cannot lock before worker assignment")
    void escrow_cannotLockBeforeAssignment() {
        JobDetailDTO job = createTestJob();

        PaymentLockRequest lockReq = new PaymentLockRequest();
        lockReq.setAmount(500.0);

        assertThatThrownBy(() -> paymentService.lockEscrow(job.getId(), savedClient.getId(), lockReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("worker assignment");
    }

    @Test
    @Order(11)
    @DisplayName("Escrow: Cannot lock twice")
    void escrow_cannotLockTwice() {
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 500.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        PaymentLockRequest lockReq = new PaymentLockRequest();
        lockReq.setAmount(500.0);
        paymentService.lockEscrow(job.getId(), savedClient.getId(), lockReq);

        // Second lock should fail
        assertThatThrownBy(() -> paymentService.lockEscrow(job.getId(), savedClient.getId(), lockReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @Order(12)
    @DisplayName("Escrow: Non-owner cannot lock")
    void escrow_nonOwnerCannotLock() {
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 500.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        // Create a different client
        Client other = Client.builder().build();
        other.setName("Other Client");
        other.setEmail("other@test.com");
        other.setPhoneNumber("+10000000200");
        other.setHashedPassword("$2a$10$hashedpassword");
        other.setCurrentAddress(savedAddress);
        other.setCurrency(Currency.USD);
        other.setVerificationStatus(new VerificationStatus(true, null, null, null));
        other.setDeleted(false);
        Client savedOther = clientRepository.save(other);

        PaymentLockRequest lockReq = new PaymentLockRequest();
        lockReq.setAmount(500.0);

        assertThatThrownBy(() -> paymentService.lockEscrow(job.getId(), savedOther.getId(), lockReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not own");
    }

    // ═══════════════════════════════════════════════════════════════
    // STATE TRANSITION GUARD TESTS
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(20)
    @DisplayName("Invalid transition: OPEN_FOR_BIDS → IN_PROGRESS blocked")
    void invalidTransition_openToInProgress() {
        JobDetailDTO job = createTestJob();

        JobStatusUpdateRequest req = new JobStatusUpdateRequest();
        req.setNewStatus(JobStatus.IN_PROGRESS);

        assertThatThrownBy(() -> jobService.updateJobStatus(job.getId(), savedClient.getId(), req))
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @Order(21)
    @DisplayName("Invalid transition: COMPLETED → anything blocked")
    void invalidTransition_completedIsTerminal() {
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 500.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        // Advance to IN_PROGRESS
        JobStatusUpdateRequest startReq = new JobStatusUpdateRequest();
        startReq.setNewStatus(JobStatus.IN_PROGRESS);
        jobService.updateJobStatusByWorker(job.getId(), savedWorker.getId(), startReq);

        // Complete the job
        JobStatusUpdateRequest completeReq = new JobStatusUpdateRequest();
        completeReq.setNewStatus(JobStatus.COMPLETED);
        jobService.updateJobStatus(job.getId(), savedClient.getId(), completeReq);

        // Try to go back — should fail
        JobStatusUpdateRequest reopenReq = new JobStatusUpdateRequest();
        reopenReq.setNewStatus(JobStatus.OPEN_FOR_BIDS);

        assertThatThrownBy(() -> jobService.updateJobStatus(job.getId(), savedClient.getId(), reopenReq))
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @Order(22)
    @DisplayName("Worker cannot transition to arbitrary states")
    void workerCannotMakeArbitraryTransition() {
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 500.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        // Worker tries to cancel (not allowed for workers)
        JobStatusUpdateRequest cancelReq = new JobStatusUpdateRequest();
        cancelReq.setNewStatus(JobStatus.CANCELLED);

        assertThatThrownBy(() -> jobService.updateJobStatusByWorker(job.getId(), savedWorker.getId(), cancelReq))
                .hasMessageContaining("Workers can only");
    }

    // ═══════════════════════════════════════════════════════════════
    // REVIEW GUARD TESTS
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(30)
    @DisplayName("Review: Cannot review non-completed job")
    void review_cannotReviewOpenJob() {
        JobDetailDTO job = createTestJob();

        ReviewCreateRequest req = new ReviewCreateRequest();
        req.setRating(5.0);
        req.setReviewComment("Great!");

        assertThatThrownBy(() -> reviewService.submitReview(job.getId(), savedClient.getId(), req))
                .hasMessageContaining("PAYMENT_RELEASED or COMPLETED");
    }

    @Test
    @Order(31)
    @DisplayName("Review: Duplicate review blocked")
    void review_duplicateBlocked() {
        // Get a job to COMPLETED state
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 500.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        JobStatusUpdateRequest startReq = new JobStatusUpdateRequest();
        startReq.setNewStatus(JobStatus.IN_PROGRESS);
        jobService.updateJobStatusByWorker(job.getId(), savedWorker.getId(), startReq);

        JobStatusUpdateRequest completeReq = new JobStatusUpdateRequest();
        completeReq.setNewStatus(JobStatus.COMPLETED);
        jobService.updateJobStatus(job.getId(), savedClient.getId(), completeReq);

        // First review succeeds
        ReviewCreateRequest req = new ReviewCreateRequest();
        req.setRating(4.0);
        req.setReviewComment("Good");
        reviewService.submitReview(job.getId(), savedClient.getId(), req);

        // Duplicate fails
        assertThatThrownBy(() -> reviewService.submitReview(job.getId(), savedClient.getId(), req))
                .hasMessageContaining("already been submitted");
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    private JobDetailDTO createTestJob() {
        JobCreateRequest jobReq = new JobCreateRequest();
        jobReq.setTitle("Test Job " + System.nanoTime());
        jobReq.setShortDescription("Short desc");
        jobReq.setLongDescription("Detailed description of the test job");
        jobReq.setPrice(TestFixtures.priceDTO(800.0));
        jobReq.setJobLocationId(savedAddress.getAddressId());
        jobReq.setJobUrgency(JobUrgency.NORMAL);
        jobReq.setRequiredSkillIds(List.of(savedSkill.getId()));
        jobReq.setPaymentMode(PaymentMode.ESCROW);
        return jobService.createJob(savedClient.getId(), jobReq);
    }

    private BidResponseDTO placeBid(Long jobId, double amount) {
        BidCreateRequest bidReq = new BidCreateRequest();
        bidReq.setBidAmount(amount);
        bidReq.setNotes("I can handle this job");
        return bidService.placeBid(jobId, savedWorker.getId(), bidReq);
    }
}
