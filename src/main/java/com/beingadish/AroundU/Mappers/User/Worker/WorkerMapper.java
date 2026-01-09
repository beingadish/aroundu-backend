package com.beingadish.AroundU.Mappers.User.Worker;

import com.beingadish.AroundU.DTO.Job.WorkerBriefDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerDetailDTO;
import com.beingadish.AroundU.DTO.Worker.WorkerSignupRequestDTO;
import com.beingadish.AroundU.Entities.Worker;
import com.beingadish.AroundU.Mappers.Common.AddressMapper;
import com.beingadish.AroundU.Models.WorkerModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface WorkerMapper {

        Worker modelToEntity(WorkerModel model);

        WorkerModel entityToModel(Worker entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "engagedJobList", ignore = true)
    @Mapping(target = "overallRating", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "hashedPassword", ignore = true)
    Worker toEntity(WorkerSignupRequestDTO dto);

    @Mapping(
            target = "engagedJobIds",
            expression = "java(entity.getEngagedJobList() == null ? java.util.List.of() : entity.getEngagedJobList().stream().map(com.beingadish.AroundU.Entities.Job::getId).toList())"
    )
    @Mapping(target = "skills", expression = "java(Collections.emptyList())")
    WorkerDetailDTO toDetailDto(Worker entity);

    @Mapping(
            target = "engagedJobIds",
            expression = "java(entity.getEngagedJobList() == null ? java.util.List.of() : entity.getEngagedJobList().stream().map(com.beingadish.AroundU.Entities.Job::getId).toList())"
    )
    WorkerModel toModel(Worker entity);

    WorkerBriefDTO toBriefDto(Worker entity);

    List<WorkerDetailDTO> toDetailDtoList(List<Worker> entities);

    List<WorkerModel> toModelList(List<Worker> entities);
}