package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Constants.Enums.JobCodeStatus;
import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.JobConfirmationCode;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Mappers.Job.JobConfirmationCodeMapper;
import com.beingadish.AroundU.Repository.Job.JobConfirmationCodeRepository;
import com.beingadish.AroundU.Repository.Job.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobCodeServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobConfirmationCodeRepository codeRepository;

    @Mock
    private JobConfirmationCodeMapper codeMapper;

    @InjectMocks
    private JobCodeServiceImpl jobCodeService;

    private Client client;
    private Worker worker;
    private Job job;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);

        worker = new Worker();
        worker.setId(2L);

        job = Job.builder()
                .id(10L)
                .createdBy(client)
                .assignedTo(worker)
                .jobStatus(JobStatus.READY_TO_START)
                .build();
    }

    @Test
    void generateCodes_createsNewCodesWhenOwnerAndStatusAllowed() {
        JobConfirmationCode mapped = JobConfirmationCode.builder()
                .job(job)
                .startCode("111111")
                .releaseCode("222222")
                .status(JobCodeStatus.START_PENDING)
                .build();

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(codeRepository.findByJob(job)).thenReturn(Optional.empty());
        when(codeMapper.create(eq(job), anyString(), anyString())).thenReturn(mapped);
        when(codeRepository.save(mapped)).thenReturn(mapped);

        JobConfirmationCode result = jobCodeService.generateCodes(job.getId(), client.getId());

        assertSame(mapped, result);
        verify(codeRepository).save(mapped);
    }

    @Test
    void generateCodes_returnsExistingWithoutSaving() {
        JobConfirmationCode existing = JobConfirmationCode.builder()
                .job(job)
                .startCode("333333")
                .releaseCode("444444")
                .status(JobCodeStatus.START_PENDING)
                .build();

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(codeRepository.findByJob(job)).thenReturn(Optional.of(existing));

        JobConfirmationCode result = jobCodeService.generateCodes(job.getId(), client.getId());

        assertSame(existing, result);
        verify(codeRepository, never()).save(any());
        verify(codeMapper, never()).create(any(), anyString(), anyString());
    }

    @Test
    void generateCodes_rejectsWhenClientDoesNotOwnJob() {
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        assertThrows(IllegalStateException.class, () -> jobCodeService.generateCodes(job.getId(), 99L));
    }

    @Test
    void verifyStartCode_updatesStatusesWhenWorkerMatchesAndCodeValid() {
        JobConfirmationCode confirmation = JobConfirmationCode.builder()
                .job(job)
                .startCode("123456")
                .releaseCode("654321")
                .status(JobCodeStatus.START_PENDING)
                .build();

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(codeRepository.findByJob(job)).thenReturn(Optional.of(confirmation));
        when(codeRepository.save(confirmation)).thenReturn(confirmation);

        JobConfirmationCode result = jobCodeService.verifyStartCode(job.getId(), worker.getId(), "123456");

        assertSame(confirmation, result);
        assertEquals(JobCodeStatus.RELEASE_PENDING, confirmation.getStatus());
        assertEquals(JobStatus.IN_PROGRESS, job.getJobStatus());
        verify(jobRepository).save(job);
        verify(codeRepository).save(confirmation);
    }

    @Test
    void verifyStartCode_rejectsWhenWorkerDoesNotMatch() {
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        JobConfirmationCode confirmation = JobConfirmationCode.builder()
                .job(job)
                .startCode("123456")
                .releaseCode("654321")
                .status(JobCodeStatus.START_PENDING)
                .build();
        when(codeRepository.findByJob(job)).thenReturn(Optional.of(confirmation));

        assertThrows(IllegalStateException.class, () -> jobCodeService.verifyStartCode(job.getId(), 999L, "123456"));
    }

    @Test
    void verifyReleaseCode_completesWhenOwnerAndStatusValid() {
        job.setJobStatus(JobStatus.IN_PROGRESS);
        JobConfirmationCode confirmation = JobConfirmationCode.builder()
                .job(job)
                .startCode("123456")
                .releaseCode("654321")
                .status(JobCodeStatus.RELEASE_PENDING)
                .build();

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(codeRepository.findByJob(job)).thenReturn(Optional.of(confirmation));
        when(codeRepository.save(confirmation)).thenReturn(confirmation);

        JobConfirmationCode result = jobCodeService.verifyReleaseCode(job.getId(), client.getId(), "654321");

        assertSame(confirmation, result);
        assertEquals(JobCodeStatus.COMPLETED, confirmation.getStatus());
        assertEquals(JobStatus.COMPLETED, job.getJobStatus());
        verify(jobRepository).save(job);
        verify(codeRepository).save(confirmation);
    }

    @Test
    void verifyReleaseCode_rejectsWhenStatusNotReleasePending() {
        job.setJobStatus(JobStatus.IN_PROGRESS);
        JobConfirmationCode confirmation = JobConfirmationCode.builder()
                .job(job)
                .startCode("123456")
                .releaseCode("654321")
                .status(JobCodeStatus.START_PENDING)
                .build();

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(codeRepository.findByJob(job)).thenReturn(Optional.of(confirmation));

        assertThrows(IllegalStateException.class, () -> jobCodeService.verifyReleaseCode(job.getId(), client.getId(), "654321"));
    }
}
