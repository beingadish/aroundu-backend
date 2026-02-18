package com.beingadish.AroundU.Config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configures Redisson client and Bloom filters for efficient duplicate
 * detection.
 * <p>
 * Three Bloom filters are initialized:
 * <ul>
 * <li><b>bidBloomFilter</b> – detects duplicate bids (worker+job)</li>
 * <li><b>profileViewBloomFilter</b> – tracks hourly profile views</li>
 * <li><b>emailRegistrationBloomFilter</b> – pre-checks email uniqueness</li>
 * </ul>
 */
@Configuration
@Profile("!test")
@Slf4j
public class RedissonConfig {

    public static final String BID_BLOOM_FILTER = "bloom:bid:duplicates";
    public static final String PROFILE_VIEW_BLOOM_FILTER = "bloom:profile:views";
    public static final String EMAIL_REGISTRATION_BLOOM_FILTER = "bloom:email:registrations";

    private static final long BID_EXPECTED_INSERTIONS = 1_000_000L;
    private static final double BID_FALSE_POSITIVE_RATE = 0.01;

    private static final long PROFILE_VIEW_EXPECTED_INSERTIONS = 1_000_000L;
    private static final double PROFILE_VIEW_FALSE_POSITIVE_RATE = 0.01;

    private static final long EMAIL_EXPECTED_INSERTIONS = 1_000_000L;
    private static final double EMAIL_FALSE_POSITIVE_RATE = 0.01;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;

        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setConnectionMinimumIdleSize(5)
                .setConnectionPoolSize(20)
                .setTimeout(5000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        log.info("Initializing RedissonClient with address: {}", address);
        return Redisson.create(config);
    }

    @Bean
    public RBloomFilter<String> bidBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(BID_BLOOM_FILTER);
        boolean initialized = bloomFilter.tryInit(BID_EXPECTED_INSERTIONS, BID_FALSE_POSITIVE_RATE);
        if (initialized) {
            log.info("Bid Bloom filter initialized: capacity={}, falsePosRate={}",
                    BID_EXPECTED_INSERTIONS, BID_FALSE_POSITIVE_RATE);
        } else {
            log.info("Bid Bloom filter already exists, reusing existing filter");
        }
        return bloomFilter;
    }

    @Bean
    public RBloomFilter<String> profileViewBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(PROFILE_VIEW_BLOOM_FILTER);
        boolean initialized = bloomFilter.tryInit(PROFILE_VIEW_EXPECTED_INSERTIONS, PROFILE_VIEW_FALSE_POSITIVE_RATE);
        if (initialized) {
            log.info("Profile view Bloom filter initialized: capacity={}, falsePosRate={}",
                    PROFILE_VIEW_EXPECTED_INSERTIONS, PROFILE_VIEW_FALSE_POSITIVE_RATE);
        } else {
            log.info("Profile view Bloom filter already exists, reusing existing filter");
        }
        return bloomFilter;
    }

    @Bean
    public RBloomFilter<String> emailRegistrationBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(EMAIL_REGISTRATION_BLOOM_FILTER);
        boolean initialized = bloomFilter.tryInit(EMAIL_EXPECTED_INSERTIONS, EMAIL_FALSE_POSITIVE_RATE);
        if (initialized) {
            log.info("Email registration Bloom filter initialized: capacity={}, falsePosRate={}",
                    EMAIL_EXPECTED_INSERTIONS, EMAIL_FALSE_POSITIVE_RATE);
        } else {
            log.info("Email registration Bloom filter already exists, reusing existing filter");
        }
        return bloomFilter;
    }
}
