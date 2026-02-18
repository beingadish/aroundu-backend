package com.beingadish.AroundU.Service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.beingadish.AroundU.infrastructure.cache.impl.RedisCacheEvictionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisCacheEvictionService")
class RedisCacheEvictionServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RedisCacheEvictionService evictionService;

    // ── evictJobDetail ───────────────────────────────────────────────────
    @Nested
    @DisplayName("evictJobDetail")
    class EvictJobDetailTests {

        @Test
        @DisplayName("deletes the specific Redis key for the given job ID")
        void deletesSpecificKey() {
            when(redisTemplate.delete(anyString())).thenReturn(true);

            evictionService.evictJobDetail(42L);

            verify(redisTemplate).delete("job:detail::42");
        }

        @Test
        @DisplayName("does nothing for null job ID")
        void nullJobId() {
            evictionService.evictJobDetail(null);

            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("handles Redis exception gracefully")
        void handlesException() {
            when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Connection lost"));

            assertDoesNotThrow(() -> evictionService.evictJobDetail(42L));
        }
    }

    // ── evictClientJobsCaches ────────────────────────────────────────────
    @Nested
    @DisplayName("evictClientJobsCaches")
    class EvictClientJobsTests {

        @Test
        @DisplayName("does nothing for null client ID")
        void nullClientId() {
            evictionService.evictClientJobsCaches(null);

            verify(redisTemplate, never()).scan(any(ScanOptions.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("scans and deletes matching keys for client")
        void scansAndDeletesKeys() {
            Cursor<String> mockCursor = mock(Cursor.class);
            doAnswer(invocation -> {
                Consumer<String> consumer = invocation.getArgument(0);
                consumer.accept("job:client:list::5:123456");
                consumer.accept("job:client:list::5:past:0:20");
                return null;
            }).when(mockCursor).forEachRemaining(any());
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(mockCursor);
            when(redisTemplate.delete(anyCollection())).thenReturn(2L);

            evictionService.evictClientJobsCaches(5L);

            verify(redisTemplate).scan(any(ScanOptions.class));
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<String>> keysCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(redisTemplate).delete(keysCaptor.capture());
            assertEquals(2, keysCaptor.getValue().size());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("does not call delete when no keys match")
        void noMatchingKeys() {
            Cursor<String> mockCursor = mock(Cursor.class);
            doAnswer(invocation -> null).when(mockCursor).forEachRemaining(any());
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(mockCursor);

            evictionService.evictClientJobsCaches(5L);

            verify(redisTemplate).scan(any(ScanOptions.class));
            verify(redisTemplate, never()).delete(anyCollection());
        }

        @Test
        @DisplayName("handles Redis exception during scan gracefully")
        void handlesScanException() {
            when(redisTemplate.scan(any(ScanOptions.class))).thenThrow(new RuntimeException("Connection lost"));

            assertDoesNotThrow(() -> evictionService.evictClientJobsCaches(5L));
        }
    }

    // ── evictWorkerFeedCaches ────────────────────────────────────────────
    @Nested
    @DisplayName("evictWorkerFeedCaches")
    class EvictWorkerFeedTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("scans and deletes all worker feed keys")
        void scansAndDeletesKeys() {
            Cursor<String> mockCursor = mock(Cursor.class);
            doAnswer(invocation -> {
                Consumer<String> consumer = invocation.getArgument(0);
                consumer.accept("job:worker:feed::1:-12345");
                return null;
            }).when(mockCursor).forEachRemaining(any());
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(mockCursor);
            when(redisTemplate.delete(anyCollection())).thenReturn(1L);

            evictionService.evictWorkerFeedCaches();

            verify(redisTemplate).scan(any(ScanOptions.class));
            verify(redisTemplate).delete(anyCollection());
        }
    }
}
