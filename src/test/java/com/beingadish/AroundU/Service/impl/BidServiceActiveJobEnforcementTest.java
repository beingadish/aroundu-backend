package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.bid.dto.BidCreateRequest;
import com.beingadish.AroundU.bid.mapper.BidMapper;
import com.beingadish.AroundU.bid.repository.BidRepository;
import com.beingadish.AroundU.bid.service.BidDuplicateCheckService;
import com.beingadish.AroundU.bid.service.impl.BidServiceImpl;
import com.beingadish.AroundU.chat.repository.ConversationRepository;
import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.infrastructure.metrics.MetricsService;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidServiceActiveJobEnforcementTest {

    @Mock
    private BidRepository bidRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private BidMapper bidMapper;
    @Mock
    private MetricsService metricsService;
    @Mock
    private BidDuplicateCheckService bidDuplicateCheckService;

    @InjectMocks
    private BidServiceImpl bidService;

    private Worker worker;
    private Job job;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setIsOnDuty(true);
        worker.setCancellationCount(0);
        worker.setBlockedUntil(null);

        Client client = new Client();
        client.setId(2L);

        job = Job.builder()
                .id(10L)
                .createdBy(client)
                .jobStatus(JobStatus.OPEN_FOR_BIDS)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void placeBid_rejectsWhenWorkerHasActiveJob() {
        // Setup timer to execute the lambda directly
        Timer mockTimer = mock(Timer.class);
        when(metricsService.getBidPlacementTimer()).thenReturn(mockTimer);
        when(mockTimer.record(any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<?> supplier = inv.getArgument(0);
                    return supplier.get();
                });

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(jobRepository.hasActiveJobAsWorker(eq(1L), any(Collection.class))).thenReturn(true);

        BidCreateRequest request = new BidCreateRequest();
        request.setBidAmount(100.0);

        assertThrows(IllegalStateException.class,
                () -> bidService.placeBid(10L, 1L, request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void placeBid_rejectsBlockedWorker() {
        Timer mockTimer = mock(Timer.class);
        when(metricsService.getBidPlacementTimer()).thenReturn(mockTimer);
        when(mockTimer.record(any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<?> supplier = inv.getArgument(0);
                    return supplier.get();
                });

        worker.setBlockedUntil(java.time.LocalDateTime.now().plusDays(5));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        BidCreateRequest request = new BidCreateRequest();
        request.setBidAmount(100.0);

        assertThrows(IllegalStateException.class,
                () -> bidService.placeBid(10L, 1L, request));
    }
}
