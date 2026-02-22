package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.user.service.impl.WorkerPenaltyServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerPenaltyServiceImplTest {

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private WorkerPenaltyServiceImpl penaltyService;

    private Worker worker;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setCancellationCount(0);
        worker.setIsOnDuty(true);

        ReflectionTestUtils.setField(penaltyService, "cancellationThreshold", 3);
        ReflectionTestUtils.setField(penaltyService, "blockDays", 7);
    }

    @Test
    void recordCancellation_incrementsCount() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        penaltyService.recordCancellation(1L);

        assertEquals(1, worker.getCancellationCount());
        assertNull(worker.getBlockedUntil());
    }

    @Test
    void recordCancellation_blocksWorkerAtThreshold() {
        worker.setCancellationCount(2); // one more = 3 = threshold
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(workerRepository.save(worker)).thenReturn(worker);

        penaltyService.recordCancellation(1L);

        assertEquals(3, worker.getCancellationCount());
        assertNotNull(worker.getBlockedUntil());
        assertFalse(worker.getIsOnDuty());
        assertTrue(worker.getBlockedUntil().isAfter(LocalDateTime.now().plusDays(6)));
    }

    @Test
    void recordCancellation_throwsForUnknownWorker() {
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> penaltyService.recordCancellation(99L));
    }

    @Test
    void isBlocked_returnsTrueWhenBlocked() {
        worker.setBlockedUntil(LocalDateTime.now().plusDays(3));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertTrue(penaltyService.isBlocked(1L));
    }

    @Test
    void isBlocked_returnsFalseWhenNotBlocked() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertFalse(penaltyService.isBlocked(1L));
    }

    @Test
    void isBlocked_returnsFalseWhenBlockExpired() {
        worker.setBlockedUntil(LocalDateTime.now().minusDays(1));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

        assertFalse(penaltyService.isBlocked(1L));
    }

    @Test
    void unblockExpiredWorkers_unblocksAndResetsCount() {
        Worker blockedWorker = new Worker();
        blockedWorker.setId(2L);
        blockedWorker.setCancellationCount(3);
        blockedWorker.setBlockedUntil(LocalDateTime.now().minusHours(1));

        when(workerRepository.findBlockedWorkersWithExpiredPenalty(any(LocalDateTime.class)))
                .thenReturn(List.of(blockedWorker));
        when(workerRepository.save(blockedWorker)).thenReturn(blockedWorker);

        penaltyService.unblockExpiredWorkers();

        assertNull(blockedWorker.getBlockedUntil());
        assertEquals(0, blockedWorker.getCancellationCount());
        verify(workerRepository).save(blockedWorker);
    }

    @Test
    void unblockExpiredWorkers_doesNothingWhenNoBlockedWorkers() {
        when(workerRepository.findBlockedWorkersWithExpiredPenalty(any(LocalDateTime.class)))
                .thenReturn(List.of());

        penaltyService.unblockExpiredWorkers();

        verify(workerRepository, never()).save(any());
    }
}
