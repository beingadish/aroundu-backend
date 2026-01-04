package com.beingadish.AroundU.Mappers;

import com.beingadish.AroundU.DTO.Job.WorkerBriefDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Models.WorkerModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class WorkerMapper {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private SkillMapper skillMapper;

    // WorkerSignupRequestDTO to Model
    public WorkerModel signupRequestDtoToModel(WorkerSignupRequestDTO dto) {
        if (dto == null) return null;

        WorkerModel model = new WorkerModel();
        model.setName(dto.getName());
        model.setEmail(dto.getEmail());
        model.setPhoneNumber(dto.getPhoneNumber());
        model.setCurrency(dto.getCurrency());
        model.setCurrentAddress(addressMapper.dtoToModel(dto.getCurrentAddress()));

        // Note: skillIds will need to be resolved to skills in service layer

        return model;
    }

    // WorkerDetailDTO to Model
    public WorkerModel workerDetailDtoToModel(WorkerDetailDTO dto) {
        if (dto == null) return null;

        WorkerModel model = new WorkerModel();
        // Copy base user fields
        model.setId(dto.getId());
        model.setName(dto.getName());
        model.setEmail(dto.getEmail());
        model.setPhoneNumber(dto.getPhoneNumber());
        model.setCurrency(dto.getCurrency());
        model.setCurrentAddress(addressMapper.dtoToModel(dto.getCurrentAddress()));

        // Worker-specific fields
        model.setEngagedJobIds(dto.getEngagedJobIds());

        return model;
    }

    // Model to WorkerDetailDTO
    public WorkerDetailDTO modelToWorkerDetailDto(WorkerModel model) {
        if (model == null) return null;

        WorkerDetailDTO dto = new WorkerDetailDTO();
        // Copy base user fields
        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setEmail(model.getEmail());
        dto.setPhoneNumber(model.getPhoneNumber());
        dto.setCurrency(model.getCurrency());
        dto.setCurrentAddress(addressMapper.modelToDto(model.getCurrentAddress()));

        // Worker-specific fields
        dto.setEngagedJobIds(model.getEngagedJobIds());
        // Note: skills will need to be resolved from service layer

        return dto;
    }

    // Model to WorkerBriefDTO
    public WorkerBriefDTO modelToWorkerBriefDto(WorkerModel model) {
        if (model == null) return null;

        WorkerBriefDTO dto = new WorkerBriefDTO();
        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setPhoneNumber(model.getPhoneNumber());
        return dto;
    }

    // Entity to Model
    public WorkerModel entityToModel(Worker entity) {
        if (entity == null) return null;

        WorkerModel model = new WorkerModel();
        // Copy base user fields
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setEmail(entity.getEmail());
        model.setPhoneNumber(entity.getPhoneNumber());
        model.setCurrency(entity.getCurrency());
        model.setCurrentAddress(addressMapper.entityToModel(entity.getCurrentAddress()));

        // Worker-specific fields
        if (entity.getEngagedJobList() != null) {
            model.setEngagedJobIds(entity.getEngagedJobList().stream()
                    .map(job -> job.getId())
                    .collect(Collectors.toList()));
        }

        return model;
    }

    // Model to Entity
    public Worker modelToEntity(WorkerModel model) {
        if (model == null) return null;

        Worker entity = new Worker();
        // Copy base user fields
        userMapper.mapModelToEntity(model, entity);

        // Note: engagedJobList will need to be resolved in service layer

        return entity;
    }

    // Entity to WorkerBriefDTO
    public WorkerBriefDTO entityToWorkerBriefDto(Worker entity) {
        if (entity == null) return null;

        WorkerBriefDTO dto = new WorkerBriefDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPhoneNumber(entity.getPhoneNumber());
        return dto;
    }
}
