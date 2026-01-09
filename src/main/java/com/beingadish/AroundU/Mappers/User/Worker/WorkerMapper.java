package com.beingadish.AroundU.Mappers.User.Worker;

import com.beingadish.AroundU.DTO.Job.WorkerBriefDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Mappers.Common.AddressMapper;
import com.beingadish.AroundU.Mappers.Common.VerificationStatusMapper;
import com.beingadish.AroundU.Models.WorkerModel;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, VerificationStatusMapper.class}, builder = @Builder(disableBuilder = true), unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "engagedJobIds", ignore = true)
    @Mapping(target = "hashedPassword", ignore = true)
    @Mapping(target = "profileImageUrl", ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "experienceYears", ignore = true)
    @Mapping(target = "certifications", ignore = true)
    @Mapping(target = "isOnDuty", ignore = true)
    @Mapping(target = "payoutAccount", ignore = true)
    WorkerModel signupRequestDtoToModel(WorkerSignupRequestDTO dto);


        @Mapping(target = "engagedJobList", ignore = true)
        @Mapping(target = "overallRating", ignore = true)
        @Mapping(target = "reviews", ignore = true)
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        @Mapping(target = "verificationStatus", ignore = true)
        Worker modelToEntity(WorkerModel model);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "engagedJobList", ignore = true)
    @Mapping(target = "overallRating", ignore = true)
    @Mapping(target = "reviews", ignore = true)
        @Mapping(target = "verificationStatus", ignore = true)
        @Mapping(target = "hashedPassword", ignore = true)
        @Mapping(target = "profileImageUrl", ignore = true)
        @Mapping(target = "experienceYears", ignore = true)
        @Mapping(target = "certifications", ignore = true)
        @Mapping(target = "isOnDuty", ignore = true)
        @Mapping(target = "payoutAccount", ignore = true)
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
    Worker toEntity(WorkerSignupRequestDTO dto);

    @Mapping(
            target = "engagedJobIds",
            expression = "java(entity.getEngagedJobList() == null ? java.util.List.of() : entity.getEngagedJobList().stream().map(com.beingadish.AroundU.Entities.Job::getId).toList())"
    )
        @Mapping(target = "skills", expression = "java(java.util.Collections.emptyList())")
    WorkerDetailDTO toDetailDto(Worker entity);

    @Mapping(
            target = "engagedJobIds",
            expression = "java(entity.getEngagedJobList() == null ? java.util.List.of() : entity.getEngagedJobList().stream().map(com.beingadish.AroundU.Entities.Job::getId).toList())"
    )
        @Mapping(target = "verificationStatus", source = "verificationStatus")
        @Mapping(target = "profileImageUrl", source = "profileImageUrl")
        @Mapping(target = "experienceYears", source = "experienceYears")
        @Mapping(target = "certifications", source = "certifications")
        @Mapping(target = "isOnDuty", source = "isOnDuty")
        @Mapping(target = "payoutAccount", source = "payoutAccount")
    WorkerModel toModel(Worker entity);

        @Mapping(target = "skills", expression = "java(java.util.Collections.emptyList())")
        WorkerDetailDTO modelToWorkerDetailDto(WorkerModel model);

    WorkerBriefDTO toBriefDto(Worker entity);

    List<WorkerDetailDTO> toDetailDtoList(List<Worker> entities);

    List<WorkerModel> toModelList(List<Worker> entities);
}