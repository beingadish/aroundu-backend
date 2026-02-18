package com.beingadish.AroundU.Controller.Client;

import com.beingadish.AroundU.Config.TestWebSecurityConfig;
import com.beingadish.AroundU.Constants.Enums.Country;
import com.beingadish.AroundU.Constants.Enums.Currency;
import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.Update.ClientUpdateRequestDTO;
import com.beingadish.AroundU.DTO.Common.AddressDTO;
import com.beingadish.AroundU.Security.UserPrincipal;
import com.beingadish.AroundU.Service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import java.util.List;
import java.util.ArrayList;

import com.beingadish.AroundU.Repository.Admin.AdminRepository;
import com.beingadish.AroundU.Repository.Address.AddressRepository;
import com.beingadish.AroundU.Repository.Bid.BidRepository;
import com.beingadish.AroundU.Repository.Client.ClientReadRepository;
import com.beingadish.AroundU.Repository.Client.ClientRepository;
import com.beingadish.AroundU.Repository.Client.ClientWriteRepository;
import com.beingadish.AroundU.Repository.Job.JobConfirmationCodeRepository;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import com.beingadish.AroundU.Repository.Payment.PaymentTransactionRepository;
import com.beingadish.AroundU.Repository.Skill.SkillRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerReadRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerRepository;
import com.beingadish.AroundU.Repository.Worker.WorkerWriteRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ClientController.class, excludeAutoConfiguration = {
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
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientService clientService;

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
    private PaymentTransactionRepository paymentTransactionRepository;

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
    private WorkerReadRepository workerReadRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private WorkerWriteRepository workerWriteRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private WorkerRepository workerRepository;

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

    @BeforeEach
    void stubEntityManager() {
        when(sharedEntityManager.getDelegate()).thenReturn(new Object());
        when(entityManagerFactory.createEntityManager()).thenReturn(sharedEntityManager);
    }

    @Test
    void registerClientReturns201AndInvokesService() throws Exception {
        ClientRegisterRequestDTO request = new ClientRegisterRequestDTO();
        request.setName("Jane Client");
        request.setEmail("jane@example.com");
        request.setPhoneNumber("+1234567890");
        request.setPassword("P@ssw0rd!");
        request.setCurrency(Currency.USD);
        request.setCurrentAddress(AddressDTO.builder()
                .country(Country.US)
                .postalCode("12345")
                .fullAddress("123 Test St")
                .build());

        mockMvc.perform(post("/api/v1/client/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"));

        ArgumentCaptor<ClientRegisterRequestDTO> captor = ArgumentCaptor.forClass(ClientRegisterRequestDTO.class);
        verify(clientService).registerClient(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void getClientDetailsAsSelfReturnsData() throws Exception {
        ClientDetailsResponseDTO dto = new ClientDetailsResponseDTO();
        when(clientService.getClientDetails(5L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/client/5")
                .with(authenticatedUser(5L, "ROLE_CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isMap());

        verify(clientService).getClientDetails(5L);
    }

    @Test
    void getClientDetailsForbiddenForDifferentClient() throws Exception {
        mockMvc.perform(get("/api/v1/client/10")
                .with(authenticatedUser(1L, "ROLE_CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListClients() throws Exception {
        List<ClientDetailsResponseDTO> content = new ArrayList<>();
        content.add(new ClientDetailsResponseDTO());
        content.add(new ClientDetailsResponseDTO());
        Page<ClientDetailsResponseDTO> page = new PageImpl<>(content);
        when(clientService.getAllClients(0, 2)).thenReturn(page);

        mockMvc.perform(get("/api/v1/client/all?page=0&size=2")
                .with(authenticatedUser(99L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2));
    }

    @Test
    void adminCanDeleteClient() throws Exception {
        mockMvc.perform(delete("/api/v1/client/42")
                .with(authenticatedUser(99L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(clientService).deleteClient(42L);
    }

    @Test
    void clientCanUpdateSelf() throws Exception {
        ClientUpdateRequestDTO update = new ClientUpdateRequestDTO();
        ClientDetailsResponseDTO response = new ClientDetailsResponseDTO();
        when(clientService.updateClientDetails(7L, update)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/client/update/7")
                .with(authenticatedUser(7L, "ROLE_CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(clientService).updateClientDetails(7L, update);
    }

    private RequestPostProcessor authenticatedUser(Long id, String role) {
        UserPrincipal principal = UserPrincipal.builder()
                .id(id)
                .email("user@example.com")
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
