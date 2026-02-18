package com.beingadish.AroundU.unit.service;

import com.beingadish.AroundU.common.constants.enums.BidStatus;
import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.bid.dto.BidCreateRequest;
import com.beingadish.AroundU.bid.dto.BidHandshakeRequest;
import com.beingadish.AroundU.bid.dto.BidResponseDTO;
import com.beingadish.AroundU.bid.entity.Bid;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.bid.mapper.BidMapper;
import com.beingadish.AroundU.bid.repository.BidRepository;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.bid.service.BidDuplicateCheckService;
import com.beingadish.AroundU.infrastructure.metrics.MetricsService;
import com.beingadish.AroundU.bid.service.impl.BidServiceImpl;
import com.beingadish.AroundU.fixtures.JobTestBuilder;
import com.beingadish.AroundU.fixtures.TestFixtures;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidServiceImpl")
class BidServiceImplTest {

    @Mock
    private BidRepository bidRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private BidMapper bidMapper;
    @Mock
    private MetricsService metricsService;
    @Mock
    private BidDuplicateCheckService bidDuplicateCheckService;

    @InjectMocks
    private BidServiceImpl bidService;

    private Client client;
    private Worker worker;
    private Job openJob;
    private Bid bid;
    private BidResponseDTO bidResponseDTO;

    @BeforeEach
    void setUp() {
        client = TestFixtures.client();
        worker = TestFixtures.worker();
        openJob = TestFixtures.job();
        bid = TestFixtures.bid(200L, openJob, worker);
        bidResponseDTO = new BidResponseDTO();
        bidResponseDTO.setId(200L);
        bidResponseDTO.setJobId(100L);
        bidResponseDTO.setWorkerId(10L);
        bidResponseDTO.setBidAmount(450.0);
        bidResponseDTO.setStatus(BidStatus.PENDING);

        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);
        lenient().when(metricsService.getBidPlacementTimer()).thenReturn(timer);
        lenient().when(metricsService.getBidsPlacedCounter()).thenReturn(counter);
        lenient().when(metricsService.getBidsAcceptedCounter()).thenReturn(counter);
        lenient().when(metricsService.getBidsRejectedCounter()).thenReturn(counter);
        lenient().when(metricsService.recordTimer(any(Timer.class), any())).thenAnswer(inv -> {
            java.util.function.Supplier<?> supplier = inv.getArgument(1);
            return supplier.get();
        });
    }

    // ── Place Bid ────────────────────────────────────────────────
    @Nested
    @DisplayName("placeBid")
    class PlaceBid {

        @Test
        @DisplayName("success – valid bid placed")
        void placeBid_Success() {
            when(jobRepository.findById(100L)).thenReturn(Optional.of(openJob));
            when(workerRepository.findById(10L)).thenReturn(Optional.of(worker));
            when(bidMapper.toEntity(any(BidCreateRequest.class), any(), any())).thenReturn(bid);
            when(bidRepository.save(any(Bid.class))).thenReturn(bid);
            when(bidMapper.toDto(any(Bid.class))).thenReturn(bidResponseDTO);

            BidResponseDTO result = bidService.placeBid(100L, 10L, TestFixtures.bidCreateRequest(450.0));

            assertNotNull(result);
            assertEquals(200L, result.getId());
            assertEquals(BidStatus.PENDING, result.getStatus());
        }

        @Test
        @DisplayName("job not found throws EntityNotFoundException")
        void placeBid_JobNotFound() {
            when(jobRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> bidService.placeBid(999L, 10L, TestFixtures.bidCreateRequest(450.0)));
        }

        @Test
        @DisplayName("worker not found throws EntityNotFoundException")
        void placeBid_WorkerNotFound() {
            when(jobRepository.findById(100L)).thenReturn(Optional.of(openJob));
            when(workerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> bidService.placeBid(100L, 999L, TestFixtures.bidCreateRequest(450.0)));
        }

        @Test
        @DisplayName("job not open for bids throws IllegalState")
        void placeBid_JobNotOpen() {
            Job closedJob = JobTestBuilder.aJob().withStatus(JobStatus.COMPLETED).build();
            when(jobRepository.findById(100L)).thenReturn(Optional.of(closedJob));
            when(workerRepository.findById(10L)).thenReturn(Optional.of(worker));

            assertThrows(IllegalStateException.class,
                    () -> bidService.placeBid(100L, 10L, TestFixtures.bidCreateRequest(450.0)));
        }

        @Test
        @DisplayName("off-duty worker cannot bid")
        void placeBid_WorkerOffDuty() {
            Worker offDutyWorker = TestFixtures.offDutyWorker(10L);
            when(jobRepository.findById(100L)).thenReturn(Optional.of(openJob));
            when(workerRepository.findById(10L)).thenReturn(Optional.of(offDutyWorker));

            assertThrows(IllegalStateException.class,
                    () -> bidService.placeBid(100L, 10L, TestFixtures.bidCreateRequest(450.0)));
        }
    }

    // ── List Bids ────────────────────────────────────────────────
    @Nested
    @DisplayName("listBidsForJob")
    class ListBids {

        @Test
        @DisplayName("returns bids for existing job")
        void listBids_Success() {
            when(jobRepository.findById(100L)).thenReturn(Optional.of(openJob));
            when(bidRepository.findByJob(openJob)).thenReturn(List.of(bid));
            when(bidMapper.toDtoList(anyList())).thenReturn(List.of(bidResponseDTO));

            List<BidResponseDTO> result = bidService.listBidsForJob(100L);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("job not found throws EntityNotFoundException")
        void listBids_JobNotFound() {
            when(jobRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> bidService.listBidsForJob(999L));
        }
    }

    // ── Accept Bid ───────────────────────────────────────────────
    @Nested
    @DisplayName("acceptBid")
    class AcceptBid {

        @Test
        @DisplayName("success – bid accepted, others rejected, job status transitions")
        void acceptBid_Success() {
            when(bidRepository.findById(200L)).thenReturn(Optional.of(bid));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(bidRepository.rejectOtherBids(openJob, 200L)).thenReturn(0);
            when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
            when(bidMapper.toDto(any(Bid.class))).thenReturn(bidResponseDTO);

            BidResponseDTO result = bidService.acceptBid(200L, 1L);

            assertNotNull(result);
            assertEquals(BidStatus.SELECTED, bid.getStatus());
            assertEquals(JobStatus.BID_SELECTED_AWAITING_HANDSHAKE, openJob.getJobStatus());
        }

        @Test
        @DisplayName("client does not own job throws IllegalState")
        void acceptBid_WrongClient() {
            when(bidRepository.findById(200L)).thenReturn(Optional.of(bid));
            when(clientRepository.findById(999L)).thenReturn(Optional.of(TestFixtures.client(999L)));

            assertThrows(IllegalStateException.class,
                    () -> bidService.acceptBid(200L, 999L));
        }

        @Test
        @DisplayName("bid not found throws EntityNotFoundException")
        void acceptBid_NotFound() {
            when(bidRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> bidService.acceptBid(999L, 1L));
        }

        @Test
        @DisplayName("job not accepting bids throws IllegalState")
        void acceptBid_JobNotAccepting() {
            Job closedJob = JobTestBuilder.aJob().withStatus(JobStatus.COMPLETED).build();
            Bid closedBid = TestFixtures.bid(200L, closedJob, worker);
            when(bidRepository.findById(200L)).thenReturn(Optional.of(closedBid));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

            assertThrows(IllegalStateException.class,
                    () -> bidService.acceptBid(200L, 1L));
        }
    }

    // ── Handshake ────────────────────────────────────────────────
    @Nested
    @DisplayName("handshake")
    class Handshake {

        private Bid selectedBid;
        private Job handshakeJob;

        @BeforeEach
        void setUpHandshake() {
            handshakeJob = JobTestBuilder.aJob().withId(100L)
                    .withStatus(JobStatus.BID_SELECTED_AWAITING_HANDSHAKE).build();
            selectedBid = TestFixtures.selectedBid(200L, handshakeJob, worker);
        }

        @Test
        @DisplayName("accepted handshake – job moves to READY_TO_START")
        void handshake_Accepted() {
            when(bidRepository.findById(200L)).thenReturn(Optional.of(selectedBid));
            when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
            when(bidMapper.toDto(any(Bid.class))).thenReturn(bidResponseDTO);

            bidService.handshake(200L, 10L, TestFixtures.handshakeRequest(true));

            assertEquals(JobStatus.READY_TO_START, handshakeJob.getJobStatus());
            assertEquals(worker, handshakeJob.getAssignedTo());
        }

        @Test
        @DisplayName("rejected handshake – bid status becomes REJECTED")
        void handshake_Rejected() {
            when(bidRepository.findById(200L)).thenReturn(Optional.of(selectedBid));
            when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
            when(bidMapper.toDto(any(Bid.class))).thenReturn(bidResponseDTO);

            bidService.handshake(200L, 10L, TestFixtures.handshakeRequest(false));

            assertEquals(BidStatus.REJECTED, selectedBid.getStatus());
        }

        @Test
        @DisplayName("wrong worker throws IllegalState")
        void handshake_WrongWorker() {
            when(bidRepository.findById(200L)).thenReturn(Optional.of(selectedBid));

            assertThrows(IllegalStateException.class,
                    () -> bidService.handshake(200L, 999L, TestFixtures.handshakeRequest(true)));
        }

        @Test
        @DisplayName("invalid job status throws IllegalState")
        void handshake_InvalidJobStatus() {
            Job openJob = JobTestBuilder.aJob().withId(100L).withStatus(JobStatus.OPEN_FOR_BIDS).build();
            Bid bidInOpenJob = TestFixtures.selectedBid(200L, openJob, worker);
            when(bidRepository.findById(200L)).thenReturn(Optional.of(bidInOpenJob));

            assertThrows(IllegalStateException.class,
                    () -> bidService.handshake(200L, 10L, TestFixtures.handshakeRequest(true)));
        }

        @Test
        @DisplayName("non-selected bid throws IllegalState")
        void handshake_BidNotSelected() {
            Bid pendingBid = TestFixtures.bid(200L, handshakeJob, worker); // PENDING status
            when(bidRepository.findById(200L)).thenReturn(Optional.of(pendingBid));

            assertThrows(IllegalStateException.class,
                    () -> bidService.handshake(200L, 10L, TestFixtures.handshakeRequest(true)));
        }
    }
}
