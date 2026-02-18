package com.beingadish.AroundU.unit.repository;

import com.beingadish.AroundU.Constants.Enums.*;
import com.beingadish.AroundU.Entities.*;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("JobRepository")
class JobRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JobRepository jobRepository;

    private Client client;
    private Address address;
    private Skill plumbing;

    @BeforeEach
    void setUp() {
        Address clientAddr = Address.builder()
                .country(Country.US).postalCode("10001").city("New York").area("Manhattan")
                .latitude(40.7128).longitude(-74.006).fullAddress("Client Address")
                .build();
        clientAddr = entityManager.persistAndFlush(clientAddr);

        client = Client.builder().build();
        client.setName("Test Client");
        client.setEmail("client@test.com");
        client.setPhoneNumber("+1234567890");
        client.setHashedPassword("$2a$10$hashed_value_here_1234567890");
        client.setCurrentAddress(clientAddr);
        client.setCurrency(Currency.USD);
        client.setVerificationStatus(new VerificationStatus(true, LocalDateTime.now(), null, null));
        client.setDeleted(false);
        client = entityManager.persistAndFlush(client);

        address = Address.builder()
                .country(Country.US).postalCode("10001").city("New York").area("Manhattan")
                .latitude(40.7128).longitude(-74.006).fullAddress("Job Address")
                .build();
        address = entityManager.persistAndFlush(address);

        plumbing = Skill.builder().name("Plumbing").jobs(new HashSet<>()).build();
        plumbing = entityManager.persistAndFlush(plumbing);
    }

    private Job createJob(String title, JobStatus status, Set<Skill> skills) {
        Job job = Job.builder()
                .title(title)
                .shortDescription("Short desc")
                .longDescription("Long description for " + title)
                .price(new Price(Currency.USD, 500.0))
                .jobLocation(address)
                .jobStatus(status)
                .jobUrgency(JobUrgency.NORMAL)
                .paymentMode(PaymentMode.ESCROW)
                .skillSet(skills)
                .createdBy(client)
                .build();
        return entityManager.persistAndFlush(job);
    }

    @Test
    @DisplayName("findByJobStatus returns only matching status")
    void findByJobStatus_ReturnsOnlyMatchingStatus() {
        createJob("Open Job", JobStatus.OPEN_FOR_BIDS, Set.of(plumbing));
        createJob("Completed Job", JobStatus.COMPLETED, Set.of(plumbing));

        List<Job> result = jobRepository.findByJobStatus(JobStatus.OPEN_FOR_BIDS);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Open Job");
    }

    @Test
    @DisplayName("findByCreatedByIdAndJobStatusIn returns client's jobs")
    void findByCreatedByIdAndJobStatusIn() {
        createJob("Active Job", JobStatus.OPEN_FOR_BIDS, Set.of(plumbing));
        createJob("Cancelled Job", JobStatus.CANCELLED, Set.of(plumbing));

        Page<Job> result = jobRepository.findByCreatedByIdAndJobStatusIn(
                client.getId(),
                List.of(JobStatus.OPEN_FOR_BIDS, JobStatus.IN_PROGRESS),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("pagination works correctly")
    void pagination_Respected() {
        for (int i = 0; i < 5; i++) {
            createJob("Job " + i, JobStatus.OPEN_FOR_BIDS, Set.of(plumbing));
        }

        Page<Job> page0 = jobRepository.findByCreatedByIdAndJobStatusIn(
                client.getId(), List.of(JobStatus.OPEN_FOR_BIDS), PageRequest.of(0, 2));
        Page<Job> page1 = jobRepository.findByCreatedByIdAndJobStatusIn(
                client.getId(), List.of(JobStatus.OPEN_FOR_BIDS), PageRequest.of(1, 2));

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("sorting by createdAt DESC")
    void sorting_ByCreatedAt() {
        Job first = createJob("First", JobStatus.OPEN_FOR_BIDS, Set.of(plumbing));
        Job second = createJob("Second", JobStatus.OPEN_FOR_BIDS, Set.of(plumbing));

        Page<Job> result = jobRepository.findByCreatedByIdAndJobStatusIn(
                client.getId(), List.of(JobStatus.OPEN_FOR_BIDS),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(result.getContent()).hasSize(2);
        // Second created job should appear first with DESC sort
        assertThat(result.getContent().get(0).getCreatedAt())
                .isAfterOrEqualTo(result.getContent().get(1).getCreatedAt());
    }

    @Test
    @DisplayName("findByIdAndCreatedById returns job for owner")
    void findByIdAndCreatedById_Success() {
        Job job = createJob("My Job", JobStatus.OPEN_FOR_BIDS, Set.of(plumbing));

        Optional<Job> result = jobRepository.findByIdAndCreatedById(job.getId(), client.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("My Job");
    }

    @Test
    @DisplayName("findByIdAndCreatedById returns empty for non-owner")
    void findByIdAndCreatedById_NonOwner() {
        Job job = createJob("My Job", JobStatus.OPEN_FOR_BIDS, Set.of(plumbing));

        Optional<Job> result = jobRepository.findByIdAndCreatedById(job.getId(), 999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchByLocation returns location-matched jobs")
    void searchByLocation() {
        createJob("NYC Job", JobStatus.OPEN_FOR_BIDS, Set.of(plumbing));

        List<Job> result = jobRepository.searchByLocation("New York", "Manhattan");

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getJobLocation().getCity()).isEqualToIgnoringCase("New York");
    }

    @Test
    @DisplayName("findOpenJobsBySkills returns skill-matched jobs")
    void findOpenJobsBySkills() {
        createJob("Plumbing Job", JobStatus.OPEN_FOR_BIDS, Set.of(plumbing));

        Page<Job> result = jobRepository.findOpenJobsBySkills(
                JobStatus.OPEN_FOR_BIDS, List.of(plumbing.getId()),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
    }
}
