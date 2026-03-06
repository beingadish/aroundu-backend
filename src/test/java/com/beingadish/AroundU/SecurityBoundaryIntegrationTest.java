package com.beingadish.AroundU;

import com.beingadish.AroundU.common.constants.enums.Country;
import com.beingadish.AroundU.common.constants.enums.Currency;
import com.beingadish.AroundU.common.entity.VerificationStatus;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.user.entity.Admin;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.repository.AdminRepository;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
class SecurityBoundaryIntegrationTest {

    private static final String CLIENT_PASSWORD = "Client#123";
    private static final String WORKER_PASSWORD = "Worker#123";
    private static final String ADMIN_PASSWORD = "Admin#123";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private WorkerRepository workerRepository;
    @Autowired
    private AdminRepository adminRepository;
    private Client clientOne;
    private Client clientTwo;
    private Worker workerOne;
    private Worker workerTwo;
    private Admin admin;

    @BeforeEach
    void setUp() {
        workerRepository.deleteAll();
        clientRepository.deleteAll();
        adminRepository.deleteAll();
        seedUsers();
    }

    @Test
    @DisplayName("Client cannot read another client's profile")
    void clientCannotReadAnotherClient() throws Exception {
        String token = loginAndGetBearer(clientOne.getEmail(), CLIENT_PASSWORD);

        mockMvc.perform(get("/api/v1/client/" + clientTwo.getId()).header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Client can read own profile")
    void clientCanReadOwnProfile() throws Exception {
        String token = loginAndGetBearer(clientOne.getEmail(), CLIENT_PASSWORD);

        mockMvc.perform(get("/api/v1/client/" + clientOne.getId()).header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(clientOne.getId().intValue())).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Admin can read any client profile")
    void adminCanReadAnyClient() throws Exception {
        String token = loginAndGetBearer(admin.getEmail(), ADMIN_PASSWORD);

        mockMvc.perform(get("/api/v1/client/" + clientTwo.getId()).header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(clientTwo.getId().intValue()));
    }

    @Test
    @DisplayName("Client list is admin-only")
    void clientListEnforced() throws Exception {
        String clientToken = loginAndGetBearer(clientOne.getEmail(), CLIENT_PASSWORD);
        mockMvc.perform(get("/api/v1/client/all?page=0&size=5").header("Authorization", clientToken)).andExpect(status().isForbidden());

        String adminToken = loginAndGetBearer(admin.getEmail(), ADMIN_PASSWORD);
        mockMvc.perform(get("/api/v1/client/all?page=0&size=5").header("Authorization", adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Admin gets 403 on /client/me")
    void adminBlockedFromClientMe() throws Exception {
        String token = loginAndGetBearer(admin.getEmail(), ADMIN_PASSWORD);

        mockMvc.perform(get("/api/v1/client/me").header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Worker cannot read another worker's profile")
    void workerCannotReadAnotherWorker() throws Exception {
        String token = loginAndGetBearer(workerOne.getEmail(), WORKER_PASSWORD);

        mockMvc.perform(get("/api/v1/worker/" + workerTwo.getId()).header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Worker can read own profile")
    void workerCanReadOwnProfile() throws Exception {
        String token = loginAndGetBearer(workerOne.getEmail(), WORKER_PASSWORD);

        mockMvc.perform(get("/api/v1/worker/" + workerOne.getId()).header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(workerOne.getId().intValue()));
    }

    @Test
    @DisplayName("Client cannot read worker profiles")
    void clientCannotReadWorker() throws Exception {
        String token = loginAndGetBearer(clientOne.getEmail(), CLIENT_PASSWORD);

        mockMvc.perform(get("/api/v1/worker/" + workerOne.getId()).header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin gets 403 on /worker/me")
    void adminBlockedFromWorkerMe() throws Exception {
        String token = loginAndGetBearer(admin.getEmail(), ADMIN_PASSWORD);

        mockMvc.perform(get("/api/v1/worker/me").header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Worker list is admin-only")
    void workerListEnforced() throws Exception {
        String workerToken = loginAndGetBearer(workerOne.getEmail(), WORKER_PASSWORD);
        mockMvc.perform(get("/api/v1/worker/all?page=0&size=5").header("Authorization", workerToken)).andExpect(status().isForbidden());

        String adminToken = loginAndGetBearer(admin.getEmail(), ADMIN_PASSWORD);
        mockMvc.perform(get("/api/v1/worker/all?page=0&size=5").header("Authorization", adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Unauthenticated requests are rejected")
    void unauthenticatedRequestsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/client/" + clientOne.getId())).andExpect(status().isUnauthorized());
    }

    private void seedUsers() {
        VerificationStatus defaultVerification = new VerificationStatus(false, null, null, LocalDateTime.now());

        clientOne = clientRepository.save(Client.builder().name("Client One").email("client1@example.com").phoneNumber("+10000000001").currency(Currency.USD).hashedPassword(passwordEncoder.encode(CLIENT_PASSWORD)).currentAddress(address("12345")).verificationStatus(defaultVerification).build());

        clientTwo = clientRepository.save(Client.builder().name("Client Two").email("client2@example.com").phoneNumber("+10000000002").currency(Currency.USD).hashedPassword(passwordEncoder.encode(CLIENT_PASSWORD)).currentAddress(address("23456")).verificationStatus(defaultVerification).build());

        workerOne = workerRepository.save(Worker.builder().name("Worker One").email("worker1@example.com").phoneNumber("+20000000001").currency(Currency.USD).hashedPassword(passwordEncoder.encode(WORKER_PASSWORD)).currentAddress(address("34567")).verificationStatus(defaultVerification).build());

        workerTwo = workerRepository.save(Worker.builder().name("Worker Two").email("worker2@example.com").phoneNumber("+20000000002").currency(Currency.USD).hashedPassword(passwordEncoder.encode(WORKER_PASSWORD)).currentAddress(address("45678")).verificationStatus(defaultVerification).build());

        admin = adminRepository.save(Admin.builder().name("Admin User").email("admin@example.com").phoneNumber("+30000000001").currency(Currency.USD).hashedPassword(passwordEncoder.encode(ADMIN_PASSWORD)).currentAddress(address("56789")).verificationStatus(defaultVerification).build());
    }

    private Address address(String postalCode) {
        return Address.builder().country(Country.US).postalCode(postalCode).city("Test City").fullAddress("Test Address").build();
    }

    private String loginAndGetBearer(String email, String password) throws Exception {
        String payload = objectMapper.writeValueAsString(new LoginPayload(email, password));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk()).andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(node.get("token").asText()).isNotBlank();
        return "Bearer " + node.get("token").asText();
    }

    private record LoginPayload(String email, String password) implements java.io.Serializable {

    }
}
