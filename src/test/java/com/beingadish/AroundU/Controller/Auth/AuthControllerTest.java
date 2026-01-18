package com.beingadish.AroundU.Controller.Auth;

import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import com.beingadish.AroundU.DTO.Auth.LoginRequestDTO;
import com.beingadish.AroundU.Entities.Admin;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Repository.Admin.AdminRepository;
import com.beingadish.AroundU.Repository.Address.AddressRepository;
import com.beingadish.AroundU.Repository.Client.ClientReadRepository;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.Client.ClientWriteRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerWriteRepository;
import com.beingadish.AroundU.Repository.Bid.BidRepository;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import com.beingadish.AroundU.Repository.Job.JobConfirmationCodeRepository;
import com.beingadish.AroundU.Repository.Payment.PaymentTransactionRepository;
import com.beingadish.AroundU.Repository.Skill.SkillRepository;
import com.beingadish.AroundU.Security.JwtTokenProvider;
import com.beingadish.AroundU.Security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(TestWebSecurityConfig.class)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
    "spring.data.jpa.repositories.enabled=false"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtTokenProvider tokenProvider;

    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ClientReadRepository clientReadRepository;

    @MockitoBean
    private WorkerReadRepository workerReadRepository;

    @MockitoBean
    private AdminRepository adminRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private WorkerWriteRepository workerWriteRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private WorkerRepository workerRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private SkillRepository skillRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private PaymentTransactionRepository paymentTransactionRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private ClientWriteRepository clientWriteRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private ClientRepository clientRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private BidRepository bidRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private JobRepository jobRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private JobConfirmationCodeRepository jobConfirmationCodeRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private AddressRepository addressRepository;

    @SuppressWarnings("unused")
    @MockitoBean(name = "entityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @SuppressWarnings("unused")
    @MockitoBean(name = "jpaSharedEM_entityManagerFactory")
    private jakarta.persistence.EntityManager sharedEntityManager;

    @SuppressWarnings("unused")
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @SuppressWarnings("unused")
    @MockitoBean
    private PlatformTransactionManager platformTransactionManager;

    @BeforeEach
    void stubEntityManager() {
        when(sharedEntityManager.getDelegate()).thenReturn(new Object());
        when(entityManagerFactory.createEntityManager()).thenReturn(sharedEntityManager);
    }

    @Test
    void authenticateClientReturnsTokenAndRole() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("client@example.com");
        request.setPassword("secret");

        Authentication authentication = authenticatedPrincipal(7L, request.getEmail(), "ROLE_CLIENT", request.getPassword());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        Client client = new Client();
        client.setId(7L);
        when(clientReadRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(client));
        when(tokenProvider.generateToken(7L, request.getEmail(), "ROLE_CLIENT")).thenReturn("jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.role").value("ROLE_CLIENT"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(clientReadRepository).findByEmail(request.getEmail());
        verify(tokenProvider).generateToken(7L, request.getEmail(), "ROLE_CLIENT");
    }

    @Test
    void authenticateWorkerResolvesWorkerId() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("worker@example.com");
        request.setPassword("secret");

        Authentication authentication = authenticatedPrincipal(15L, request.getEmail(), "ROLE_WORKER", request.getPassword());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        Worker worker = new Worker();
        worker.setId(15L);
        when(workerReadRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(worker));
        when(tokenProvider.generateToken(15L, request.getEmail(), "ROLE_WORKER")).thenReturn("jwt-worker-token");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(15))
                .andExpect(jsonPath("$.token").value("jwt-worker-token"))
                .andExpect(jsonPath("$.role").value("ROLE_WORKER"));

        verify(workerReadRepository).findByEmail(request.getEmail());
        verify(tokenProvider).generateToken(15L, request.getEmail(), "ROLE_WORKER");
    }

    @Test
    void authenticateAdminResolvesAdminId() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@example.com");
        request.setPassword("secret");

        Authentication authentication = authenticatedPrincipal(99L, request.getEmail(), "ROLE_ADMIN", request.getPassword());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        Admin admin = new Admin();
        admin.setId(99L);
        when(adminRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(admin));
        when(tokenProvider.generateToken(99L, request.getEmail(), "ROLE_ADMIN")).thenReturn("jwt-admin-token");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(99))
                .andExpect(jsonPath("$.token").value("jwt-admin-token"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));

        verify(adminRepository).findByEmail(request.getEmail());
        verify(tokenProvider).generateToken(99L, request.getEmail(), "ROLE_ADMIN");
    }

    private Authentication authenticatedPrincipal(Long id, String email, String role, String password) {
        UserPrincipal principal = UserPrincipal.builder()
                .id(id)
                .email(email)
                .password(password)
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
        return new UsernamePasswordAuthenticationToken(principal, password, principal.getAuthorities());
    }
}
