package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.Exceptions.Bid.DuplicateBidException;
import com.beingadish.AroundU.Repository.Bid.BidRepository;
import com.beingadish.AroundU.Repository.Client.ClientReadRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import com.beingadish.AroundU.Service.impl.BidDuplicateCheckServiceImpl;
import com.beingadish.AroundU.Service.impl.ProfileViewTrackingServiceImpl;
import com.beingadish.AroundU.Service.impl.RegistrationValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Bloom-filter-backed services.
 * <p>
 * Uses Mockito mocks for {@link RBloomFilter}, repositories, and Redis
 * templates so that no real Redis connection is needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bloom Filter Services")
class BloomFilterServiceTest {

    // ─── Shared mocks ────────────────────────────────────────────────────
    @Mock private BloomFilterMetricsService bloomFilterMetricsService;

    // =====================================================================
    //  BidDuplicateCheckService tests
    // =====================================================================
    @Nested
    @DisplayName("BidDuplicateCheckService")
    class BidDuplicateCheckTests {

        @Mock private RBloomFilter<String> bidBloomFilter;
        @Mock private BidRepository bidRepository;

        private BidDuplicateCheckServiceImpl service;

        @BeforeEach
        void setUp() {
            service = new BidDuplicateCheckServiceImpl(
                    bidBloomFilter, bidRepository, bloomFilterMetricsService);
        }

        @Test
        @DisplayName("should allow bid when Bloom filter says 'definitely not present'")
        void allowBid_whenBloomSaysNotPresent() {
            when(bidBloomFilter.contains(anyString())).thenReturn(false);

            service.validateNoDuplicateBid(1L, 100L);

            verify(bidBloomFilter).contains("worker:1:job:100");
            verifyNoInteractions(bidRepository);
        }

        @Test
        @DisplayName("should reject bid when Bloom filter and DB both confirm duplicate")
        void rejectBid_whenDuplicateConfirmedByDb() {
            when(bidBloomFilter.contains("worker:2:job:200")).thenReturn(true);
            when(bidRepository.existsByWorkerIdAndJobId(2L, 200L)).thenReturn(true);

            assertThatThrownBy(() -> service.validateNoDuplicateBid(2L, 200L))
                    .isInstanceOf(DuplicateBidException.class)
                    .hasMessageContaining("already bid");

            verify(bidRepository).existsByWorkerIdAndJobId(2L, 200L);
        }

        @Test
        @DisplayName("should handle false positive gracefully – allow bid when DB says no duplicate")
        void allowBid_whenBloomFalsePositive() {
            when(bidBloomFilter.contains("worker:3:job:300")).thenReturn(true);
            when(bidRepository.existsByWorkerIdAndJobId(3L, 300L)).thenReturn(false);

            // Should NOT throw
            service.validateNoDuplicateBid(3L, 300L);

            verify(bidRepository).existsByWorkerIdAndJobId(3L, 300L);
            verify(bloomFilterMetricsService).recordFalsePositive("bid");
        }

        @Test
        @DisplayName("should record bid in Bloom filter after successful placement")
        void recordBid_addsToBloomFilter() {
            service.recordBid(5L, 500L);

            verify(bidBloomFilter).add("worker:5:job:500");
        }

        @Test
        @DisplayName("should use correct key format: worker:{id}:job:{id}")
        void keyFormat_isCorrect() {
            when(bidBloomFilter.contains("worker:42:job:99")).thenReturn(false);

            service.validateNoDuplicateBid(42L, 99L);

            verify(bidBloomFilter).contains("worker:42:job:99");
        }
    }

    // =====================================================================
    //  ProfileViewTrackingService tests
    // =====================================================================
    @Nested
    @DisplayName("ProfileViewTrackingService")
    class ProfileViewTrackingTests {

        @Mock private RBloomFilter<String> profileViewBloomFilter;
        @Mock private StringRedisTemplate stringRedisTemplate;
        @Mock private ValueOperations<String, String> valueOps;

        private ProfileViewTrackingServiceImpl service;

        @BeforeEach
        void setUp() {
            service = new ProfileViewTrackingServiceImpl(
                    profileViewBloomFilter, stringRedisTemplate, bloomFilterMetricsService);
        }

        @Test
        @DisplayName("should increment counter on first view in hour")
        void firstView_incrementsCounter() {
            when(profileViewBloomFilter.contains(anyString())).thenReturn(false);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

            boolean result = service.trackView(10L, 20L);

            assertThat(result).isTrue();
            verify(profileViewBloomFilter).add(anyString());
            verify(valueOps).increment("profile:views:20");
        }

        @Test
        @DisplayName("should not increment counter on repeated view in same hour")
        void repeatedView_doesNotIncrementCounter() {
            when(profileViewBloomFilter.contains(anyString())).thenReturn(true);

            boolean result = service.trackView(10L, 20L);

            assertThat(result).isFalse();
            verifyNoInteractions(stringRedisTemplate);
        }

        @Test
        @DisplayName("should return view count from Redis")
        void getViewCount_returnsStoredValue() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("profile:views:20")).thenReturn("42");

            long count = service.getViewCount(20L);

            assertThat(count).isEqualTo(42L);
        }

        @Test
        @DisplayName("should return 0 when no view count exists")
        void getViewCount_returnsZeroWhenNull() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get("profile:views:99")).thenReturn(null);

            long count = service.getViewCount(99L);

            assertThat(count).isEqualTo(0L);
        }

        @Test
        @DisplayName("should include hour bucket in Bloom filter key")
        void keyFormat_includesHourBucket() {
            when(profileViewBloomFilter.contains(Mockito.<String>argThat(
                    s -> s != null && s.startsWith("view:10:profile:20:hour:"))))
                    .thenReturn(false);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

            service.trackView(10L, 20L);

            verify(profileViewBloomFilter).contains(Mockito.<String>argThat(
                    key -> key != null && key.matches("view:10:profile:20:hour:\\d{10}")));
            verify(profileViewBloomFilter).add(Mockito.<String>argThat(
                    key -> key != null && key.matches("view:10:profile:20:hour:\\d{10}")));
        }
    }

    // =====================================================================
    //  RegistrationValidationService tests
    // =====================================================================
    @Nested
    @DisplayName("RegistrationValidationService")
    class RegistrationValidationTests {

        @Mock private RBloomFilter<String> emailRegistrationBloomFilter;
        @Mock private ClientReadRepository clientReadRepository;
        @Mock private WorkerReadRepository workerReadRepository;

        private RegistrationValidationServiceImpl service;

        @BeforeEach
        void setUp() {
            service = new RegistrationValidationServiceImpl(
                    emailRegistrationBloomFilter, clientReadRepository,
                    workerReadRepository, bloomFilterMetricsService);
        }

        @Test
        @DisplayName("should return false (available) when Bloom says definitely not present")
        void emailAvailable_whenBloomSaysNotPresent() {
            when(emailRegistrationBloomFilter.contains("email:test@example.com")).thenReturn(false);

            boolean registered = service.isEmailAlreadyRegistered("test@example.com");

            assertThat(registered).isFalse();
            verifyNoInteractions(clientReadRepository);
            verifyNoInteractions(workerReadRepository);
        }

        @Test
        @DisplayName("should return true when Bloom says possibly present and DB confirms (client)")
        void emailRegistered_confirmedByClient() {
            when(emailRegistrationBloomFilter.contains("email:used@example.com")).thenReturn(true);
            when(clientReadRepository.existsByEmail("used@example.com")).thenReturn(true);

            boolean registered = service.isEmailAlreadyRegistered("used@example.com");

            assertThat(registered).isTrue();
        }

        @Test
        @DisplayName("should return true when Bloom says possibly present and DB confirms (worker)")
        void emailRegistered_confirmedByWorker() {
            when(emailRegistrationBloomFilter.contains("email:worker@example.com")).thenReturn(true);
            when(clientReadRepository.existsByEmail("worker@example.com")).thenReturn(false);
            when(workerReadRepository.existsByEmail("worker@example.com")).thenReturn(true);

            boolean registered = service.isEmailAlreadyRegistered("worker@example.com");

            assertThat(registered).isTrue();
        }

        @Test
        @DisplayName("should handle false positive – return false when DB says not registered")
        void emailAvailable_whenBloomFalsePositive() {
            when(emailRegistrationBloomFilter.contains("email:new@example.com")).thenReturn(true);
            when(clientReadRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(workerReadRepository.existsByEmail("new@example.com")).thenReturn(false);

            boolean registered = service.isEmailAlreadyRegistered("new@example.com");

            assertThat(registered).isFalse();
            verify(bloomFilterMetricsService).recordFalsePositive("email");
        }

        @Test
        @DisplayName("should normalise email to lowercase before checking")
        void emailNormalisedToLowerCase() {
            when(emailRegistrationBloomFilter.contains("email:upper@example.com")).thenReturn(false);

            service.isEmailAlreadyRegistered("UPPER@EXAMPLE.COM");

            verify(emailRegistrationBloomFilter).contains("email:upper@example.com");
        }

        @Test
        @DisplayName("should record registration in Bloom filter")
        void recordRegistration_addsToBloomFilter() {
            service.recordRegistration("Registered@Example.com");

            verify(emailRegistrationBloomFilter).add("email:registered@example.com");
        }

        @Test
        @DisplayName("should trim whitespace from email before checking")
        void emailTrimmed_beforeChecking() {
            when(emailRegistrationBloomFilter.contains("email:trimmed@example.com")).thenReturn(false);

            service.isEmailAlreadyRegistered("  trimmed@example.com  ");

            verify(emailRegistrationBloomFilter).contains("email:trimmed@example.com");
        }
    }

    // =====================================================================
    //  Bloom filter performance / size tests
    // =====================================================================
    @Nested
    @DisplayName("Filter Size & Performance")
    class FilterSizeTests {

        @Mock private RBloomFilter<String> bidBloomFilter;
        @Mock private BidRepository bidRepository;

        private BidDuplicateCheckServiceImpl service;

        @BeforeEach
        void setUp() {
            service = new BidDuplicateCheckServiceImpl(
                    bidBloomFilter, bidRepository, bloomFilterMetricsService);
        }

        @Test
        @DisplayName("Bloom lookup should complete within expected time (mock sanity check)")
        void bloomLookup_isFast() {
            when(bidBloomFilter.contains(anyString())).thenReturn(false);

            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                service.validateNoDuplicateBid((long) i, (long) i);
            }
            long elapsed = System.nanoTime() - start;

            // 1000 lookups should complete in well under 1 second with mocks
            assertThat(elapsed).isLessThan(1_000_000_000L);
        }

        @Test
        @DisplayName("should not query DB when Bloom filter says definitely not present (bulk)")
        void noDatabaseHit_whenBloomNegative_bulk() {
            when(bidBloomFilter.contains(anyString())).thenReturn(false);

            for (int i = 0; i < 100; i++) {
                service.validateNoDuplicateBid((long) i, (long) (i + 1000));
            }

            verifyNoInteractions(bidRepository);
        }
    }
}
