package com.beingadish.AroundU.fixtures;

import com.beingadish.AroundU.Constants.Enums.*;
import com.beingadish.AroundU.DTO.Bid.BidCreateRequest;
import com.beingadish.AroundU.DTO.Bid.BidHandshakeRequest;
import com.beingadish.AroundU.DTO.Common.PriceDTO;
import com.beingadish.AroundU.DTO.Job.*;
import com.beingadish.AroundU.DTO.Payment.PaymentLockRequest;
import com.beingadish.AroundU.DTO.Payment.PaymentReleaseRequest;
import com.beingadish.AroundU.Entities.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Central factory for commonly used test objects.
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    // ── Addresses ────────────────────────────────────────────────
    public static Address address() {
        return Address.builder()
                .addressId(1L)
                .country(Country.US)
                .postalCode("10001")
                .city("New York")
                .area("Manhattan")
                .latitude(40.7128)
                .longitude(-74.006)
                .fullAddress("123 Main St, Manhattan, New York, NY 10001")
                .build();
    }

    public static Address address(Long id, Double lat, Double lon) {
        return Address.builder()
                .addressId(id)
                .country(Country.US)
                .postalCode("10001")
                .city("New York")
                .area("Manhattan")
                .latitude(lat)
                .longitude(lon)
                .fullAddress("Address " + id)
                .build();
    }

    // ── Skills ───────────────────────────────────────────────────
    public static Skill skill(Long id, String name) {
        return Skill.builder().id(id).name(name).jobs(new HashSet<>()).build();
    }

    public static Set<Skill> plumbingSkills() {
        return Set.of(skill(1L, "Plumbing"), skill(2L, "Pipe Fitting"));
    }

    // ── VerificationStatus ───────────────────────────────────────
    public static VerificationStatus verified() {
        return new VerificationStatus(true, LocalDateTime.now(), LocalDateTime.now().plusYears(1), LocalDateTime.now());
    }

    // ── Client ───────────────────────────────────────────────────
    public static Client client() {
        return client(1L);
    }

    public static Client client(Long id) {
        Client c = Client.builder().build();
        c.setId(id);
        c.setName("Test Client " + id);
        c.setEmail("client" + id + "@test.com");
        c.setPhoneNumber("+1234567890");
        c.setHashedPassword("$2a$10$abcdefghijklmnopqrstuv");
        c.setCurrentAddress(address());
        c.setCurrency(Currency.USD);
        c.setVerificationStatus(verified());
        c.setDeleted(false);
        return c;
    }

    // ── Worker ───────────────────────────────────────────────────
    public static Worker worker() {
        return worker(10L);
    }

    public static Worker worker(Long id) {
        Worker w = Worker.builder().build();
        w.setId(id);
        w.setName("Test Worker " + id);
        w.setEmail("worker" + id + "@test.com");
        w.setPhoneNumber("+1987654321");
        w.setHashedPassword("$2a$10$abcdefghijklmnopqrstuv");
        w.setCurrentAddress(address(2L, 40.7580, -73.9855));
        w.setCurrency(Currency.USD);
        w.setVerificationStatus(verified());
        w.setDeleted(false);
        w.setIsOnDuty(true);
        w.setOverallRating(4.5);
        w.setExperienceYears(5);
        return w;
    }

    public static Worker offDutyWorker(Long id) {
        Worker w = worker(id);
        w.setIsOnDuty(false);
        return w;
    }

    // ── Price ────────────────────────────────────────────────────
    public static Price price(double amount) {
        return new Price(Currency.USD, amount);
    }

    public static PriceDTO priceDTO(double amount) {
        return PriceDTO.builder().currency(Currency.USD).amount(amount).build();
    }

    // ── Job ──────────────────────────────────────────────────────
    public static Job job() {
        return job(100L, client());
    }

    public static Job job(Long id, Client creator) {
        return Job.builder()
                .id(id)
                .title("Fix plumbing")
                .shortDescription("Kitchen sink is leaking")
                .longDescription("The kitchen sink has been leaking for 2 days")
                .price(price(500.0))
                .jobLocation(address())
                .jobStatus(JobStatus.OPEN_FOR_BIDS)
                .jobUrgency(JobUrgency.NORMAL)
                .paymentMode(PaymentMode.ESCROW)
                .skillSet(plumbingSkills())
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Job jobWithStatus(Long id, Client creator, JobStatus status) {
        Job j = job(id, creator);
        j.setJobStatus(status);
        return j;
    }

    public static Job assignedJob(Long id, Client creator, Worker worker) {
        Job j = jobWithStatus(id, creator, JobStatus.READY_TO_START);
        j.setAssignedTo(worker);
        return j;
    }

    // ── Bid ──────────────────────────────────────────────────────
    public static Bid bid(Long id, Job job, Worker worker) {
        return Bid.builder()
                .id(id)
                .job(job)
                .worker(worker)
                .bidAmount(450.0)
                .notes("I can start tomorrow")
                .status(BidStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Bid selectedBid(Long id, Job job, Worker worker) {
        Bid b = bid(id, job, worker);
        b.setStatus(BidStatus.SELECTED);
        return b;
    }

    // ── PaymentTransaction ───────────────────────────────────────
    public static PaymentTransaction escrowLocked(Long id, Job job, Client client, Worker worker) {
        return PaymentTransaction.builder()
                .id(id)
                .job(job)
                .client(client)
                .worker(worker)
                .amount(500.0)
                .paymentMode(PaymentMode.ESCROW)
                .status(PaymentStatus.ESCROW_LOCKED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── JobConfirmationCode ──────────────────────────────────────
    public static JobConfirmationCode confirmationCode(Job job) {
        return JobConfirmationCode.builder()
                .id(1L)
                .job(job)
                .startCode("START123")
                .releaseCode("RELEASE456")
                .status(JobCodeStatus.START_PENDING)
                .build();
    }

    // ── DTOs ─────────────────────────────────────────────────────
    public static JobCreateRequest jobCreateRequest() {
        JobCreateRequest req = new JobCreateRequest();
        req.setTitle("Fix plumbing");
        req.setShortDescription("Kitchen sink is leaking");
        req.setLongDescription("The kitchen sink has been leaking for 2 days");
        req.setPrice(priceDTO(500.0));
        req.setJobLocationId(1L);
        req.setJobUrgency(JobUrgency.NORMAL);
        req.setRequiredSkillIds(List.of(1L, 2L));
        req.setPaymentMode(PaymentMode.ESCROW);
        return req;
    }

    public static JobUpdateRequest jobUpdateRequest() {
        JobUpdateRequest req = new JobUpdateRequest();
        req.setTitle("Fix plumbing - updated");
        return req;
    }

    public static JobStatusUpdateRequest statusUpdateRequest(JobStatus status) {
        JobStatusUpdateRequest req = new JobStatusUpdateRequest();
        req.setNewStatus(status);
        return req;
    }

    public static BidCreateRequest bidCreateRequest(double amount) {
        BidCreateRequest req = new BidCreateRequest();
        req.setBidAmount(amount);
        req.setNotes("I can handle this");
        return req;
    }

    public static BidHandshakeRequest handshakeRequest(boolean accepted) {
        BidHandshakeRequest req = new BidHandshakeRequest();
        req.setAccepted(accepted);
        return req;
    }

    public static PaymentLockRequest paymentLockRequest(double amount) {
        PaymentLockRequest req = new PaymentLockRequest();
        req.setAmount(amount);
        return req;
    }

    public static PaymentReleaseRequest paymentReleaseRequest(String code) {
        PaymentReleaseRequest req = new PaymentReleaseRequest();
        req.setReleaseCode(code);
        return req;
    }

    public static WorkerJobFeedRequest workerFeedRequest() {
        WorkerJobFeedRequest req = new WorkerJobFeedRequest();
        req.setSkillIds(List.of(1L, 2L));
        req.setRadiusKm(25.0);
        req.setPage(0);
        req.setSize(20);
        return req;
    }

    public static JobFilterRequest jobFilterRequest() {
        JobFilterRequest req = new JobFilterRequest();
        req.setPage(0);
        req.setSize(20);
        return req;
    }

    // ── JobDetailDTO ─────────────────────────────────────────────
    public static JobDetailDTO jobDetailDTO() {
        JobDetailDTO dto = new JobDetailDTO();
        dto.setId(100L);
        dto.setTitle("Fix plumbing");
        dto.setJobStatus(JobStatus.OPEN_FOR_BIDS);
        dto.setJobUrgency(JobUrgency.NORMAL);
        dto.setPaymentMode(PaymentMode.ESCROW);
        dto.setPrice(priceDTO(500.0));
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}
