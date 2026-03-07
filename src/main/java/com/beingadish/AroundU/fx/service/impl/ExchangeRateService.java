package com.beingadish.AroundU.fx.service.impl;

import com.beingadish.AroundU.fx.dto.ExchangeRateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches live currency exchange rates from the Frankfurter API
 * (https://api.frankfurter.app) — free, no API key required. Rates are cached
 * per pair for 1 hour to avoid unnecessary external calls.
 */
@Service
@Slf4j
public class ExchangeRateService {

    private static final String FRANKFURTER_BASE = "https://api.frankfurter.app";
    private static final long CACHE_TTL_SECONDS = 3600; // 1 hour

    private final RestClient restClient = RestClient.builder()
            .baseUrl(FRANKFURTER_BASE)
            .build();

    private record CachedRate(double rate, Instant fetchedAt) {

    }

    private final Map<String, CachedRate> cache = new ConcurrentHashMap<>();

    /**
     * Returns the exchange rate from {@code from} to {@code to}. Returns 1.0 if
     * the currencies are equal or the API call fails.
     */
    public ExchangeRateDTO getRate(String from, String to) {
        if (from == null || to == null) {
            return ExchangeRateDTO.builder().from(from).to(to).rate(1.0).build();
        }
        final String fromUpper = from.trim().toUpperCase();
        final String toUpper = to.trim().toUpperCase();

        if (fromUpper.equals(toUpper)) {
            return ExchangeRateDTO.builder().from(fromUpper).to(toUpper).rate(1.0).build();
        }

        final String key = fromUpper + "_" + toUpper;
        final CachedRate cached = cache.get(key);
        if (cached != null && Instant.now().minusSeconds(CACHE_TTL_SECONDS).isBefore(cached.fetchedAt())) {
            return ExchangeRateDTO.builder().from(fromUpper).to(toUpper).rate(cached.rate()).build();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/latest?from={from}&to={to}", fromUpper, toUpper)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                log.warn("Null response from Frankfurter for {}->{}", fromUpper, toUpper);
                return ExchangeRateDTO.builder().from(fromUpper).to(toUpper).rate(null).build();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            if (rates == null || !rates.containsKey(toUpper)) {
                log.warn("No rate found for {} in Frankfurter response", toUpper);
                return ExchangeRateDTO.builder().from(fromUpper).to(toUpper).rate(null).build();
            }

            double rate = ((Number) rates.get(toUpper)).doubleValue();
            cache.put(key, new CachedRate(rate, Instant.now()));
            log.debug("Fetched rate {} -> {}: {}", fromUpper, toUpper, rate);
            return ExchangeRateDTO.builder().from(fromUpper).to(toUpper).rate(rate).build();

        } catch (Exception e) {
            log.error("Failed to fetch exchange rate {} -> {}: {}", fromUpper, toUpper, e.getMessage());
            return ExchangeRateDTO.builder().from(fromUpper).to(toUpper).rate(null).build();
        }
    }
}
