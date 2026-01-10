package com.beingadish.AroundU.Config;

import com.beingadish.AroundU.Security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] AUTH_WHITELIST = {
            "/api/v1/auth/**",
            "/api/v1/client/register",
            "/api/v1/worker/register"
    };
    private static final String[] ADMIN_LIST = {
            "/api/v1/admin/**",
            "/api/v1/jobs/**",
            "/api/v1/payments/**"
    };
    private static final String[] CLIENT_LIST = {
            "/api/v1/client/**",
            "/api/v1/jobs/**",          // e.g., create/list jobs
            "/api/v1/bids/**",          // if clients view bids on their jobs
            "/api/v1/payments/**"       // if clients manage payments
    };
    private static final String[] WORKER_LIST = {
            "/api/v1/worker/**",
            "/api/v1/bids/**",          // place/manage bids
            "/api/v1/jobs/**",          // jobs assigned to them / browse allowed subset
            "/api/v1/payments/**"       // payouts etc.
    };
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        // Admin-only endpoints first
                        .requestMatchers("/api/v1/client/all", "/api/v1/worker/all").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Allow both admin and client on client endpoints
                        .requestMatchers("/api/v1/client/**").hasAnyRole("CLIENT", "ADMIN")
                        // Allow both admin and worker on worker endpoints
                        .requestMatchers("/api/v1/worker/**").hasAnyRole("WORKER", "ADMIN")
                        // Shared resources: allow admin or relevant role
                        .requestMatchers("/api/v1/jobs/**").hasAnyRole("CLIENT", "WORKER", "ADMIN")
                        .requestMatchers("/api/v1/bids/**").hasAnyRole("CLIENT", "WORKER", "ADMIN")
                        .requestMatchers("/api/v1/payments/**").hasAnyRole("CLIENT", "WORKER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
