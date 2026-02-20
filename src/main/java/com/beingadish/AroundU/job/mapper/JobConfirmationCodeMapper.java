package com.beingadish.AroundU.job.mapper;

import com.beingadish.AroundU.common.constants.enums.JobCodeStatus;
import com.beingadish.AroundU.job.dto.JobCodeResponseDTO;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.entity.JobConfirmationCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobConfirmationCodeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", source = "job")
    @Mapping(target = "status", expression = "java(com.beingadish.AroundU.common.constants.enums.JobCodeStatus.START_PENDING)")
    JobConfirmationCode create(Job job, String startCode, String releaseCode);

    @Mapping(target = "jobId", source = "job.id")
    JobCodeResponseDTO toDto(JobConfirmationCode entity);

    /**
     * Maps to DTO showing only the start code (for client after generation).
     */
    default JobCodeResponseDTO toDtoWithStartCodeOnly(JobConfirmationCode entity) {
        JobCodeResponseDTO dto = toDto(entity);
        dto.setReleaseCode(null);
        return dto;
    }

    /**
     * Maps to DTO showing only the release code (for client during release
     * step).
     */
    default JobCodeResponseDTO toDtoWithReleaseCodeOnly(JobConfirmationCode entity) {
        JobCodeResponseDTO dto = toDto(entity);
        dto.setStartCode(null);
        return dto;
    }

    /**
     * Maps to DTO hiding both codes (for worker — they verify codes, not see
     * them).
     */
    default JobCodeResponseDTO toDtoWithoutCodes(JobConfirmationCode entity) {
        JobCodeResponseDTO dto = toDto(entity);
        dto.setStartCode(null);
        dto.setReleaseCode(null);
        return dto;
    }
}
