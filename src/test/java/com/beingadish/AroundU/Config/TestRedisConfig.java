package com.beingadish.AroundU.Config;

import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@Profile("test")
public class TestRedisConfig {

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        // Provide a deep-stubbed mock so geo operations can be invoked without Redis.
        return Mockito.mock(StringRedisTemplate.class, Answers.RETURNS_DEEP_STUBS);
    }
}
