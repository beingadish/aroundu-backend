package com.beingadish.AroundU.e2e;

import com.beingadish.AroundU.common.constants.enums.*;
import com.beingadish.AroundU.bid.dto.BidCreateRequest;
import com.beingadish.AroundU.bid.dto.BidHandshakeRequest;
import com.beingadish.AroundU.bid.dto.BidResponseDTO;
import com.beingadish.AroundU.chat.dto.ChatMessageRequest;
import com.beingadish.AroundU.chat.dto.ChatMessageResponseDTO;
import com.beingadish.AroundU.chat.dto.ConversationResponseDTO;
import com.beingadish.AroundU.chat.entity.Conversation;
import com.beingadish.AroundU.chat.repository.ConversationRepository;
import com.beingadish.AroundU.chat.service.ChatService;
import com.beingadish.AroundU.job.dto.*;
import com.beingadish.AroundU.job.entity.JobConfirmationCode;
import com.beingadish.AroundU.job.repository.JobConfirmationCodeRepository;
import com.beingadish.AroundU.job.service.JobCodeService;
import com.beingadish.AroundU.job.service.JobService;
import com.beingadish.AroundU.bid.service.BidService;
import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import com.beingadish.AroundU.payment.service.PaymentService;
import com.beingadish.AroundU.review.dto.ReviewCreateRequest;
import com.beingadish.AroundU.review.dto.ReviewResponseDTO;
import com.beingadish.AroundU.review.service.ReviewService;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.common.entity.VerificationStatus;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.common.entity.Skill;
import com.beingadish.AroundU.location.repository.AddressRepository;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.common.repository.SkillRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.fixtures.TestFixtures;
import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Complete end-to-end lifecycle test covering every stage: Job creation →
 * Bidding → Acceptance → Handshake → Escrow → OTP code generation → Start code
 * verification → Chat messaging → Work completion → Release code verification →
 * Reviews
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestWebSecurityConfig.class)
@Transactional
@DisplayName("Complete Job Lifecycle E2E Test")
class CompleteJobLifecycleTest {

    @Autowired
    private JobService jobService;
    @Autowired
    private BidService bidService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ChatService chatService;
    @Autowired
    private JobCodeService jobCodeService;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private JobConfirmationCodeRepository codeRepository;
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
        Address clientAddr = Address.builder()
                .country(Country.US).postalCode("10001").city("New York")
                .area("Manhattan").latitude(40.7128).longitude(-74.006)
                .fullAddress("123 Main St").build();
        savedAddress = addressRepository.save(clientAddr);

        Address workerAddr = Address.builder()
                .country(Country.US).postalCode("10002").city("New York")
                .area("Brooklyn").latitude(40.6782).longitude(-73.9442)
                .fullAddress("456 Worker Ave").build();
        Address savedWorkerAddr = addressRepository.save(workerAddr);

        Client c = Client.builder().build();
        c.setName("E2E Client");
        c.setEmail("e2e-client@test.com");
        c.setPhoneNumber("+10000000300");
        c.setHashedPassword("$2a$10$hashedpassword");
        c.setCurrentAddress(savedAddress);
        c.setCurrency(Currency.USD);
        c.setVerificationStatus(new VerificationStatus(true, null, null, null));
        c.setDeleted(false);
        savedClient = clientRepository.save(c);

        Worker w = Worker.builder().build();
        w.setName("E2E Worker");
        w.setEmail("e2e-worker@test.com");
        w.setPhoneNumber("+10000000301");
        w.setHashedPassword("$2a$10$hashedpassword");
        w.setCurrentAddress(savedWorkerAddr);
        w.setCurrency(Currency.USD);
        w.setVerificationStatus(new VerificationStatus(true, null, null, null));
        w.setDeleted(false);
        w.setIsOnDuty(true);
        w.setOverallRating(4.5);
        w.setExperienceYears(5);
        savedWorker = workerRepository.save(w);

        savedSkill = skillRepository.save(Skill.builder().name("Electrical").build());
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPLETE LIFECYCLE: Job → Bid → Accept → Handshake → Escrow →
    //   OTP → Start → Chat → Complete → Release → Reviews
    // ═══════════════════════════════════════════════════════════════
    @Test
    @DisplayName("Full lifecycle: job posting through OTP, chat, completion, and reviews")
    void completeJobLifecycle() {
        // ── STEP 1: Client creates job ──
        JobDetailDTO job = createTestJob();
        assertThat(job.getId()).isNotNull();
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.OPEN_FOR_BIDS);

        // ── STEP 2: Worker places bid ──
        BidResponseDTO bid = placeBid(job.getId(), 750.0);
        assertThat(bid.getId()).isNotNull();
        assertThat(bid.getStatus()).isEqualTo(BidStatus.PENDING);

        // No conversation exists yet
        List<Conversation> convoBefore = conversationRepository.findByJobId(job.getId());
        assertThat(convoBefore).isEmpty();

        // ── STEP 3: Client accepts bid ──
        bidService.acceptBid(bid.getId(), savedClient.getId());

        // Conversation auto-created on acceptance
        List<Conversation> convoAfter = conversationRepository.findByJobId(job.getId());
        assertThat(convoAfter).hasSize(1);
        Conversation conversation = convoAfter.get(0);
        assertThat(conversation.getParticipantOneId()).isEqualTo(savedClient.getId());
        assertThat(conversation.getParticipantTwoId()).isEqualTo(savedWorker.getId());

        // Job status transitions to BID_SELECTED_AWAITING_HANDSHAKE
        JobDetailDTO afterAccept = jobService.getJobDetail(job.getId());
        assertThat(afterAccept.getJobStatus()).isEqualTo(JobStatus.BID_SELECTED_AWAITING_HANDSHAKE);

        // ── STEP 4: Client generates OTP codes (allowed after bid selection) ──
        JobConfirmationCode codes = jobCodeService.generateCodes(job.getId(), savedClient.getId());
        assertThat(codes.getStartCode()).isNotBlank();
        assertThat(codes.getReleaseCode()).isNotBlank();
        assertThat(codes.getStatus()).isEqualTo(JobCodeStatus.START_PENDING);
        String startCode = codes.getStartCode();
        String releaseCode = codes.getReleaseCode();

        // ── STEP 5: Worker accepts handshake ──
        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        JobDetailDTO afterHandshake = jobService.getJobDetail(job.getId());
        assertThat(afterHandshake.getJobStatus()).isEqualTo(JobStatus.READY_TO_START);

        // ── STEP 6: Client locks escrow ──
        PaymentLockRequest lockReq = new PaymentLockRequest();
        lockReq.setAmount(750.0);
        PaymentTransaction payment = paymentService.lockEscrow(job.getId(), savedClient.getId(), lockReq);
        assertThat(payment).isNotNull();

        // ── STEP 7: Worker verifies START code → job becomes IN_PROGRESS ──
        JobConfirmationCode afterStart = jobCodeService.verifyStartCode(
                job.getId(), savedWorker.getId(), startCode);
        assertThat(afterStart.getStatus()).isEqualTo(JobCodeStatus.RELEASE_PENDING);

        JobDetailDTO inProgress = jobService.getJobDetail(job.getId());
        assertThat(inProgress.getJobStatus()).isEqualTo(JobStatus.IN_PROGRESS);

        // ── STEP 8: Chat messaging while job is in progress ──
        // Client sends a message to worker
        ChatMessageRequest clientMsg = new ChatMessageRequest();
        clientMsg.setRecipientId(savedWorker.getId());
        clientMsg.setContent("How is the work going?");
        ChatMessageResponseDTO sentByClient = chatService.sendMessage(
                job.getId(), savedClient.getId(), "CLIENT", clientMsg);
        assertThat(sentByClient.getContent()).isEqualTo("How is the work going?");
        assertThat(sentByClient.getSenderRole()).isEqualTo("CLIENT");
        assertThat(sentByClient.getConversationId()).isEqualTo(conversation.getId());

        // Worker sends a reply
        ChatMessageRequest workerMsg = new ChatMessageRequest();
        workerMsg.setRecipientId(savedClient.getId());
        workerMsg.setContent("Almost done!");
        ChatMessageResponseDTO sentByWorker = chatService.sendMessage(
                job.getId(), savedWorker.getId(), "WORKER", workerMsg);
        assertThat(sentByWorker.getContent()).isEqualTo("Almost done!");
        assertThat(sentByWorker.getSenderRole()).isEqualTo("WORKER");

        // Client has 1 unread message (from worker)
        List<ConversationResponseDTO> clientConvos = chatService.getConversations(
                savedClient.getId(), "CLIENT");
        assertThat(clientConvos).hasSize(1);
        assertThat(clientConvos.get(0).getUnreadCount()).isEqualTo(1);

        // Worker has 1 unread message (from client)
        List<ConversationResponseDTO> workerConvos = chatService.getConversations(
                savedWorker.getId(), "WORKER");
        assertThat(workerConvos).hasSize(1);
        assertThat(workerConvos.get(0).getUnreadCount()).isEqualTo(1);

        // Worker marks messages as read
        List<Long> readIds = chatService.markAsRead(
                conversation.getId(), savedWorker.getId(), "WORKER");
        assertThat(readIds).containsExactly(sentByClient.getId());

        // Worker unread count now 0
        workerConvos = chatService.getConversations(savedWorker.getId(), "WORKER");
        assertThat(workerConvos.get(0).getUnreadCount()).isEqualTo(0);

        // Client marks as delivered then read
        List<Long> deliveredIds = chatService.markAsDelivered(
                conversation.getId(), savedClient.getId(), "CLIENT");
        assertThat(deliveredIds).containsExactly(sentByWorker.getId());

        List<Long> clientReadIds = chatService.markAsRead(
                conversation.getId(), savedClient.getId(), "CLIENT");
        assertThat(clientReadIds).containsExactly(sentByWorker.getId());

        // ── STEP 9: Worker verifies RELEASE code → job completes ──
        JobConfirmationCode afterRelease = jobCodeService.verifyReleaseCode(
                job.getId(), savedWorker.getId(), releaseCode);
        assertThat(afterRelease.getStatus()).isEqualTo(JobCodeStatus.COMPLETED);

        JobDetailDTO completed = jobService.getJobDetail(job.getId());
        assertThat(completed.getJobStatus()).isEqualTo(JobStatus.COMPLETED);

        // ── STEP 10: Both parties leave reviews ──
        ReviewCreateRequest clientReview = new ReviewCreateRequest();
        clientReview.setRating(5.0);
        clientReview.setReviewComment("Excellent work, very professional!");
        ReviewResponseDTO cReview = reviewService.submitReview(
                job.getId(), savedClient.getId(), clientReview);
        assertThat(cReview.getRating()).isEqualTo(5.0);
        assertThat(cReview.getJobId()).isEqualTo(job.getId());

        // Duplicate client review blocked
        assertThatThrownBy(() -> reviewService.submitReview(
                job.getId(), savedClient.getId(), clientReview))
                .hasMessageContaining("already been submitted");
    }

    @Test
    @DisplayName("OTP: wrong start code increments attempts")
    void otpWrongStartCodeIncrementsAttempts() {
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 600.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());

        jobCodeService.generateCodes(job.getId(), savedClient.getId());

        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        assertThatThrownBy(()
                -> jobCodeService.verifyStartCode(job.getId(), savedWorker.getId(), "WRONG1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid start code");
    }

    @Test
    @DisplayName("OTP: cannot verify start code before READY_TO_START")
    void otpCannotVerifyStartBeforeReady() {
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 600.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());

        JobConfirmationCode codes = jobCodeService.generateCodes(job.getId(), savedClient.getId());

        // Job is BID_SELECTED_AWAITING_HANDSHAKE, not READY_TO_START
        assertThatThrownBy(()
                -> jobCodeService.verifyStartCode(job.getId(), savedWorker.getId(), codes.getStartCode()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ready to start");
    }

    @Test
    @DisplayName("OTP: cannot verify release code before start code")
    void otpCannotVerifyReleaseBeforeStart() {
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 600.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());

        JobConfirmationCode codes = jobCodeService.generateCodes(job.getId(), savedClient.getId());

        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        // Start code not yet verified → release should fail
        assertThatThrownBy(()
                -> jobCodeService.verifyReleaseCode(job.getId(), savedWorker.getId(), codes.getReleaseCode()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Start code not yet confirmed");
    }

    @Test
    @DisplayName("Chat: cannot send message on job without assigned worker")
    void chatCannotSendWithoutAssignedWorker() {
        JobDetailDTO job = createTestJob();

        ChatMessageRequest msg = new ChatMessageRequest();
        msg.setRecipientId(savedWorker.getId());
        msg.setContent("Hello");

        assertThatThrownBy(()
                -> chatService.sendMessage(job.getId(), savedClient.getId(), "CLIENT", msg))
                .hasMessageContaining("without an assigned worker");
    }

    @Test
    @DisplayName("Chat: sender role correctly distinguishes client and worker messages")
    void chatSenderRoleDistinction() {
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 500.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());

        BidHandshakeRequest hsReq = new BidHandshakeRequest();
        hsReq.setAccepted(true);
        bidService.handshake(bid.getId(), savedWorker.getId(), hsReq);

        // Client → Worker
        ChatMessageRequest clientMsg = new ChatMessageRequest();
        clientMsg.setRecipientId(savedWorker.getId());
        clientMsg.setContent("Client message");
        ChatMessageResponseDTO fromClient = chatService.sendMessage(
                job.getId(), savedClient.getId(), "CLIENT", clientMsg);

        // Worker → Client
        ChatMessageRequest workerMsg = new ChatMessageRequest();
        workerMsg.setRecipientId(savedClient.getId());
        workerMsg.setContent("Worker message");
        ChatMessageResponseDTO fromWorker = chatService.sendMessage(
                job.getId(), savedWorker.getId(), "WORKER", workerMsg);

        assertThat(fromClient.getSenderRole()).isEqualTo("CLIENT");
        assertThat(fromWorker.getSenderRole()).isEqualTo("WORKER");
        assertThat(fromClient.getConversationId()).isEqualTo(fromWorker.getConversationId());
    }

    @Test
    @DisplayName("Worker handshake reject: bid becomes REJECTED")
    void workerRejectsHandshake() {
        JobDetailDTO job = createTestJob();
        BidResponseDTO bid = placeBid(job.getId(), 500.0);
        bidService.acceptBid(bid.getId(), savedClient.getId());

        BidHandshakeRequest rejectReq = new BidHandshakeRequest();
        rejectReq.setAccepted(false);
        bidService.handshake(bid.getId(), savedWorker.getId(), rejectReq);

        JobDetailDTO afterReject = jobService.getJobDetail(job.getId());
        // Worker assignment is cleared but job stays in BID_SELECTED_AWAITING_HANDSHAKE
        assertThat(afterReject.getJobStatus()).isEqualTo(JobStatus.BID_SELECTED_AWAITING_HANDSHAKE);
        assertThat(afterReject.getAssignedTo()).isNull();
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════
    private JobDetailDTO createTestJob() {
        JobCreateRequest jobReq = new JobCreateRequest();
        jobReq.setTitle("E2E Lifecycle Job " + System.nanoTime());
        jobReq.setShortDescription("End-to-end test job");
        jobReq.setLongDescription("Comprehensive lifecycle test covering all stages");
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
        bidReq.setNotes("I can handle this job professionally");
        return bidService.placeBid(jobId, savedWorker.getId(), bidReq);
    }
}
