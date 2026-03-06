package com.beingadish.AroundU.job.mapper;

import com.beingadish.AroundU.common.entity.Skill;
import com.beingadish.AroundU.common.mapper.AddressMapper;
import com.beingadish.AroundU.common.mapper.PriceMapper;
import com.beingadish.AroundU.common.mapper.SkillMapper;
import com.beingadish.AroundU.job.dto.JobCreateRequest;
import com.beingadish.AroundU.job.dto.JobDetailDTO;
import com.beingadish.AroundU.job.dto.JobSummaryDTO;
import com.beingadish.AroundU.job.dto.JobUpdateRequest;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.job.model.JobModel;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.mapper.UserMapper;
import com.beingadish.AroundU.user.mapper.WorkerMapper;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, PriceMapper.class, SkillMapper.class, UserMapper.class, WorkerMapper.class})
public interface JobMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "price", source = "request.price")
    @Mapping(target = "jobLocation", source = "jobLocation")
    @Mapping(target = "jobStatus", expression = "java(com.beingadish.AroundU.common.constants.enums.JobStatus.CREATED)")
    @Mapping(target = "jobUrgency", source = "request.jobUrgency")
    @Mapping(target = "paymentMode", source = "request.paymentMode")
    @Mapping(target = "skillSet", source = "skills")
    @Mapping(target = "createdBy", source = "creator")
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "scheduledStartTime", ignore = true)
    Job toEntity(JobCreateRequest request, Address jobLocation, Set<Skill> skills, Client creator);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "price", source = "request.price")
    @Mapping(target = "jobLocation", source = "jobLocation")
    @Mapping(target = "jobUrgency", source = "request.jobUrgency")
    @Mapping(target = "jobStatus", source = "request.jobStatus")
    @Mapping(target = "paymentMode", source = "request.paymentMode")
    @Mapping(target = "skillSet", source = "skills")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "scheduledStartTime", ignore = true)
    void updateEntity(JobUpdateRequest request, @MappingTarget Job job, Address jobLocation, Set<Skill> skills);

    @Mapping(target = "requiredSkills", source = "skillSet")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "assignedTo", source = "assignedTo")
    JobDetailDTO toDetailDto(Job entity);

    @Mapping(target = "price", source = "price")
    @Mapping(target = "distanceKm", ignore = true)
    @Mapping(target = "popularityScore", ignore = true)
    JobSummaryDTO toSummaryDto(Job entity);

    @Mapping(target = "requiredSkills", source = "skillSet")
    @Mapping(target = "createdByUserId", expression = "java(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)")
    @Mapping(target = "assignedToWorkerId", expression = "java(entity.getAssignedTo() != null ? entity.getAssignedTo().getId() : null)")
    JobModel toModel(Job entity);

    List<JobSummaryDTO> toSummaryList(List<Job> entities);

    List<JobDetailDTO> toDetailList(List<Job> entities);

    List<JobModel> toModelList(List<Job> entities);
}
