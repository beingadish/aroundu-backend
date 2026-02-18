package com.beingadish.AroundU.Controller.Worker;

import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import com.beingadish.AroundU.Constants.Enums.Country;
import com.beingadish.AroundU.Constants.Enums.Currency;
import com.beingadish.AroundU.DTO.Common.AddressDTO;
import com.beingadish.AroundU.DTO.Worker.Update.WorkerUpdateRequestDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
import com.beingadish.AroundU.Security.UserPrincipal;
import com.beingadish.AroundU.Service.WorkerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.domain.PageRequest;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerWriteRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerRepository;
import com.beingadish.AroundU.Repository.Payment.PaymentTransactionRepository;
import com.beingadish.AroundU.Repository.Client.ClientReadRepository;
import com.beingadish.AroundU.Repository.Client.ClientWriteRepository;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.Admin.AdminRepository;
import com.beingadish.AroundU.Repository.Skill.SkillRepository;
import com.beingadish.AroundU.Repository.Bid.BidRepository;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import com.beingadish.AroundU.Repository.Job.JobConfirmationCodeRepository;
import com.beingadish.AroundU.Repository.Address.AddressRepository;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = WorkerController.class, excludeAutoConfiguration = {
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
class WorkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkerService workerService;

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

    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @SuppressWarnings("unused")
    @MockitoBean
    private WorkerWriteRepository workerWriteRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private WorkerReadRepository workerReadRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private WorkerRepository workerRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private ClientReadRepository clientReadRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private ClientWriteRepository clientWriteRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private ClientRepository clientRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private AdminRepository adminRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private SkillRepository skillRepository;

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
    @MockitoBean
    private com.beingadish.AroundU.Repository.FailedGeoSync.FailedGeoSyncRepository failedGeoSyncRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Analytics.AggregatedMetricsRepository aggregatedMetricsRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private com.beingadish.AroundU.Repository.Notification.FailedNotificationRepository failedNotificationRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private PaymentTransactionRepository paymentTransactionRepository;

    @BeforeEach
    void stubEntityManager() {
        when(sharedEntityManager.getDelegate()).thenReturn(new Object());
        when(entityManagerFactory.createEntityManager()).thenReturn(sharedEntityManager);
    }

    @Test
    void registerWorkerReturns201() throws Exception {
        WorkerSignupRequestDTO request = new WorkerSignupRequestDTO();
        request.setName("Will Worker");
        request.setEmail("worker@example.com");
        request.setPhoneNumber("+10987654321");
        request.setPassword("P@ssw0rd!");
        request.setCurrency(Currency.USD);
        request.setCurrentAddress(AddressDTO.builder()
                .country(Country.US)
                .postalCode("54321")
                .fullAddress("321 Demo Rd")
                .build());

        mockMvc.perform(post("/api/v1/worker/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"));

        verify(workerService).registerWorker(request);
    }

    @Test
    void getWorkerDetailsAsSelfReturnsData() throws Exception {
        WorkerDetailDTO dto = new WorkerDetailDTO();
        when(workerService.getWorkerDetails(3L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/worker/3")
                .with(authenticatedUser(3L, "ROLE_WORKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isMap());

        verify(workerService).getWorkerDetails(3L);
    }

    @Test
    void getWorkerDetailsForbiddenForDifferentWorker() throws Exception {
        mockMvc.perform(get("/api/v1/worker/8")
                .with(authenticatedUser(2L, "ROLE_WORKER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListWorkers() throws Exception {
        List<WorkerDetailDTO> content = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            content.add(new WorkerDetailDTO());
        }
        Page<WorkerDetailDTO> page = new PageImpl<>(content, PageRequest.of(1, 5), content.size());
        when(workerService.getAllWorkers(1, 5)).thenReturn(page);

        mockMvc.perform(get("/api/v1/worker/all?page=1&size=5")
                .with(authenticatedUser(99L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5));
    }

    @Test
    void workerCanUpdateSelf() throws Exception {
        WorkerUpdateRequestDTO update = new WorkerUpdateRequestDTO();
        WorkerDetailDTO response = new WorkerDetailDTO();
        when(workerService.updateWorkerDetails(6L, update)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/worker/update/6")
                .with(authenticatedUser(6L, "ROLE_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(workerService).updateWorkerDetails(6L, update);
    }

    @Test
    void adminCanDeleteWorker() throws Exception {
        mockMvc.perform(delete("/api/v1/worker/77")
                .with(authenticatedUser(1L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(workerService).deleteWorker(77L);
    }

    private RequestPostProcessor authenticatedUser(Long id, String role) {
        UserPrincipal principal = UserPrincipal.builder()
                .id(id)
                .email("worker@example.com")
                .password("pw")
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.setUserPrincipal(authentication);
            return SecurityMockMvcRequestPostProcessors.authentication(authentication).postProcessRequest(request);
        };
    }
}
