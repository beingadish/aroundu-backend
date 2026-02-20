package com.beingadish.AroundU.unit.service;

import com.beingadish.AroundU.common.constants.enums.PaymentStatus;
import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.dto.PaymentReleaseRequest;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import com.beingadish.AroundU.job.entity.JobConfirmationCode;
import com.beingadish.AroundU.payment.mapper.PaymentTransactionMapper;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.job.repository.JobConfirmationCodeRepository;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.payment.repository.PaymentTransactionRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.infrastructure.metrics.MetricsService;
import com.beingadish.AroundU.payment.service.impl.PaymentServiceImpl;
import com.beingadish.AroundU.fixtures.TestDataBuilder;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl")
class PaymentServiceImplTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private PaymentTransactionMapper paymentTransactionMapper;
    @Mock
    private JobConfirmationCodeRepository jobConfirmationCodeRepository;
    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Client client;
    private Worker worker;
    private Job assignedJob;
    private PaymentTransaction escrowTx;
    private JobConfirmationCode confirmationCode;

    @BeforeEach
    void setUp() {
        client = TestFixtures.client();
        worker = TestFixtures.worker();
        assignedJob = TestFixtures.assignedJob(100L, client, worker);
        escrowTx = TestFixtures.escrowLocked(300L, assignedJob, client, worker);
        confirmationCode = TestFixtures.confirmationCode(assignedJob);

        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);
        lenient().when(metricsService.getEscrowLockTimer()).thenReturn(timer);
        lenient().when(metricsService.getEscrowReleaseTimer()).thenReturn(timer);
        lenient().when(metricsService.getEscrowLockedCounter()).thenReturn(counter);
        lenient().when(metricsService.getEscrowReleasedCounter()).thenReturn(counter);
        lenient().when(metricsService.recordTimer(any(Timer.class), any())).thenAnswer(inv -> {
            java.util.function.Supplier<?> supplier = inv.getArgument(1);
            return supplier.get();
        });
    }

    // ── Lock Escrow ──────────────────────────────────────────────
    @Nested
    @DisplayName("lockEscrow")
    class LockEscrow {

        @Test
        @DisplayName("success – escrow locked for assigned job")
        void lockEscrow_Success() {
            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(paymentTransactionMapper.toEntity(any(PaymentLockRequest.class), any(), any(), any()))
                    .thenReturn(escrowTx);
            when(paymentTransactionRepository.save(any())).thenReturn(escrowTx);

            PaymentTransaction result = paymentService.lockEscrow(100L, 1L,
                    TestFixtures.paymentLockRequest(500.0));

            assertNotNull(result);
            assertEquals(PaymentStatus.ESCROW_LOCKED, result.getStatus());
        }

        @Test
        @DisplayName("job not found throws EntityNotFoundException")
        void lockEscrow_JobNotFound() {
            when(jobRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> paymentService.lockEscrow(999L, 1L, TestFixtures.paymentLockRequest(500.0)));
        }

        @Test
        @DisplayName("client not found throws EntityNotFoundException")
        void lockEscrow_ClientNotFound() {
            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(clientRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> paymentService.lockEscrow(100L, 999L, TestFixtures.paymentLockRequest(500.0)));
        }

        @Test
        @DisplayName("no worker assigned throws IllegalState")
        void lockEscrow_NoWorkerAssigned() {
            Job noWorkerJob = TestFixtures.job();
            noWorkerJob.setAssignedTo(null);
            when(jobRepository.findById(100L)).thenReturn(Optional.of(noWorkerJob));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

            assertThrows(IllegalStateException.class,
                    () -> paymentService.lockEscrow(100L, 1L, TestFixtures.paymentLockRequest(500.0)));
        }
    }

    // ── Release Escrow ───────────────────────────────────────────
    @Nested
    @DisplayName("releaseEscrow")
    class ReleaseEscrow {

        @Test
        @DisplayName("success – payment released with valid code")
        void releaseEscrow_Success() {
            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(jobConfirmationCodeRepository.findByJob(assignedJob)).thenReturn(Optional.of(confirmationCode));
            when(paymentTransactionRepository.findByJob(assignedJob)).thenReturn(Optional.of(escrowTx));
            when(paymentTransactionRepository.save(any())).thenReturn(escrowTx);

            PaymentTransaction result = paymentService.releaseEscrow(100L, 1L,
                    TestFixtures.paymentReleaseRequest("RELEASE456"));

            assertEquals(PaymentStatus.RELEASED, result.getStatus());
        }

        @Test
        @DisplayName("invalid release code throws IllegalArgument")
        void releaseEscrow_InvalidCode() {
            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(jobConfirmationCodeRepository.findByJob(assignedJob)).thenReturn(Optional.of(confirmationCode));

            assertThrows(IllegalArgumentException.class,
                    () -> paymentService.releaseEscrow(100L, 1L,
                            TestFixtures.paymentReleaseRequest("WRONG_CODE")));
        }

        @Test
        @DisplayName("wrong client cannot release")
        void releaseEscrow_WrongClient() {
            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));

            assertThrows(IllegalStateException.class,
                    () -> paymentService.releaseEscrow(100L, 999L,
                            TestFixtures.paymentReleaseRequest("RELEASE456")));
        }

        @Test
        @DisplayName("already released payment throws IllegalState")
        void releaseEscrow_AlreadyReleased() {
            PaymentTransaction releasedTx = TestDataBuilder.aPayment()
                    .withJob(assignedJob).withStatus(PaymentStatus.RELEASED).build();
            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(jobConfirmationCodeRepository.findByJob(assignedJob)).thenReturn(Optional.of(confirmationCode));
            when(paymentTransactionRepository.findByJob(assignedJob)).thenReturn(Optional.of(releasedTx));

            assertThrows(IllegalStateException.class,
                    () -> paymentService.releaseEscrow(100L, 1L,
                            TestFixtures.paymentReleaseRequest("RELEASE456")));
        }

        @Test
        @DisplayName("no confirmation code throws EntityNotFoundException")
        void releaseEscrow_NoConfirmationCode() {
            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(jobConfirmationCodeRepository.findByJob(assignedJob)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> paymentService.releaseEscrow(100L, 1L,
                            TestFixtures.paymentReleaseRequest("RELEASE456")));
        }

        @Test
        @DisplayName("no payment transaction throws EntityNotFoundException")
        void releaseEscrow_NoTransaction() {
            when(jobRepository.findById(100L)).thenReturn(Optional.of(assignedJob));
            when(jobConfirmationCodeRepository.findByJob(assignedJob)).thenReturn(Optional.of(confirmationCode));
            when(paymentTransactionRepository.findByJob(assignedJob)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> paymentService.releaseEscrow(100L, 1L,
                            TestFixtures.paymentReleaseRequest("RELEASE456")));
        }
    }
}
