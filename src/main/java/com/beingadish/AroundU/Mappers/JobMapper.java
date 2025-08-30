package com.beingadish.AroundU.Mappers;

import com.beingadish.AroundU.DTO.Job.JobCreateRequest;
import com.beingadish.AroundU.DTO.Job.JobDetailDTO;
import com.beingadish.AroundU.DTO.Job.JobSummaryDTO;
import com.beingadish.AroundU.DTO.Job.JobUpdateRequest;
import com.beingadish.AroundU.Entities.JobEntity;
import com.beingadish.AroundU.Models.JobModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    @Autowired
    private PriceMapper priceMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private SkillMapper skillMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkerMapper workerMapper;

    // JobCreateRequest to Model
    public JobModel createRequestToModel(JobCreateRequest dto) {
        if (dto == null) return null;

        JobModel model = new JobModel();
        model.setTitle(dto.getTitle());
        model.setShortDescription(dto.getShortDescription());
        model.setLongDescription(dto.getLongDescription());
        model.setPrice(priceMapper.dtoToModel(dto.getPrice()));
        model.setJobUrgency(dto.getJobUrgency());

        // Note: jobLocationId and requiredSkillIds will need to be resolved in service layer

        return model;
    }

    // JobUpdateRequest to Model
    public JobModel updateRequestToModel(JobUpdateRequest dto) {
        if (dto == null) return null;

        JobModel model = new JobModel();
        model.setTitle(dto.getTitle());
        model.setShortDescription(dto.getShortDescription());
        model.setLongDescription(dto.getLongDescription());
        model.setPrice(priceMapper.dtoToModel(dto.getPrice()));
        model.setJobUrgency(dto.getJobUrgency());
        model.setJobStatus(dto.getJobStatus());

        // Note: jobLocationId and requiredSkillIds will need to be resolved in service layer

        return model;
    }

    // JobDetailDTO to Model
    public JobModel jobDetailDtoToModel(JobDetailDTO dto) {
        if (dto == null) return null;

        JobModel model = new JobModel();
        model.setId(dto.getId());
        model.setTitle(dto.getTitle());
        model.setShortDescription(dto.getShortDescription());
        model.setLongDescription(dto.getLongDescription());
        model.setPrice(priceMapper.dtoToModel(dto.getPrice()));
        model.setJobLocation(addressMapper.dtoToModel(dto.getJobLocation()));
        model.setJobStatus(dto.getJobStatus());
        model.setJobUrgency(dto.getJobUrgency());
        model.setRequiredSkills(skillMapper.dtosToModels(dto.getRequiredSkills()));
        model.setCreatedAt(dto.getCreatedAt());
        model.setUpdatedAt(dto.getUpdatedAt());

        if (dto.getCreatedBy() != null) {
            model.setCreatedByUserId(dto.getCreatedBy().getId());
        }

        if (dto.getAssignedTo() != null) {
            model.setAssignedToWorkerId(dto.getAssignedTo().getId());
        }

        return model;
    }

    // Model to JobDetailDTO
    public JobDetailDTO modelToJobDetailDto(JobModel model) {
        if (model == null) return null;

        JobDetailDTO dto = new JobDetailDTO();
        dto.setId(model.getId());
        dto.setTitle(model.getTitle());
        dto.setShortDescription(model.getShortDescription());
        dto.setLongDescription(model.getLongDescription());
        dto.setPrice(priceMapper.modelToDto(model.getPrice()));
        dto.setJobLocation(addressMapper.modelToDto(model.getJobLocation()));
        dto.setJobStatus(model.getJobStatus());
        dto.setJobUrgency(model.getJobUrgency());
        dto.setRequiredSkills(skillMapper.modelsToDtos(model.getRequiredSkills()));
        dto.setCreatedAt(model.getCreatedAt());
        dto.setUpdatedAt(model.getUpdatedAt());

        // Note: createdBy and assignedTo will need to be resolved in service layer

        return dto;
    }

    // Model to JobSummaryDTO
    public JobSummaryDTO modelToJobSummaryDto(JobModel model) {
        if (model == null) return null;

        JobSummaryDTO dto = new JobSummaryDTO();
        dto.setId(model.getId());
        dto.setTitle(model.getTitle());
        dto.setShortDescription(model.getShortDescription());
        dto.setJobStatus(model.getJobStatus());
        dto.setJobUrgency(model.getJobUrgency());
        dto.setPrice(priceMapper.modelToDto(model.getPrice()));
        dto.setCreatedAt(model.getCreatedAt());

        return dto;
    }

    // Entity to Model
    public JobModel entityToModel(JobEntity entity) {
        if (entity == null) return null;

        JobModel model = new JobModel();
        model.setId(entity.getId());
        model.setTitle(entity.getTitle());
        model.setShortDescription(entity.getShortDescription());
        model.setLongDescription(entity.getLongDescription());
        model.setPrice(priceMapper.entityToModel(entity.getPrice()));
        model.setJobLocation(addressMapper.entityToModel(entity.getJobLocation()));
        model.setJobStatus(entity.getJobStatus());
        model.setJobUrgency(entity.getJobUrgency());
        model.setRequiredSkills(skillMapper.entitiesToModels(entity.getRequiredSkills()));
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCreatedBy() != null) {
            model.setCreatedByUserId(entity.getCreatedBy().getId());
        }

        if (entity.getAssignedTo() != null) {
            model.setAssignedToWorkerId(entity.getAssignedTo().getId());
        }

        return model;
    }

    // Model to Entity
    public JobEntity modelToEntity(JobModel model) {
        if (model == null) return null;

        JobEntity entity = new JobEntity();
        entity.setId(model.getId());
        entity.setTitle(model.getTitle());
        entity.setShortDescription(model.getShortDescription());
        entity.setLongDescription(model.getLongDescription());
        entity.setPrice(priceMapper.modelToEntity(model.getPrice()));
        entity.setJobLocation(addressMapper.modelToEntity(model.getJobLocation()));
        entity.setJobStatus(model.getJobStatus());
        entity.setJobUrgency(model.getJobUrgency());
        entity.setRequiredSkills(skillMapper.modelsToEntities(model.getRequiredSkills()));
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());

        // Note: createdBy and assignedTo entities will need to be resolved in service layer

        return entity;
    }

    // Entity to JobSummaryDTO (direct conversion)
    public JobSummaryDTO entityToJobSummaryDto(JobEntity entity) {
        if (entity == null) return null;

        JobSummaryDTO dto = new JobSummaryDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setShortDescription(entity.getShortDescription());
        dto.setJobStatus(entity.getJobStatus());
        dto.setJobUrgency(entity.getJobUrgency());
        dto.setPrice(priceMapper.entityToDto(entity.getPrice()));
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }
}
