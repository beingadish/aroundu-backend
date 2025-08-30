package com.beingadish.AroundU.Mappers;

import com.beingadish.AroundU.DTO.Common.SkillDTO;
import com.beingadish.AroundU.Entities.SkillEntity;
import com.beingadish.AroundU.Models.SkillModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SkillMapper {

    // DTO to Model
    public SkillModel dtoToModel(SkillDTO dto) {
        if (dto == null) return null;

        SkillModel model = new SkillModel();
        model.setId(dto.getId());
        model.setSkillName(dto.getSkillName());
        return model;
    }

    // Model to DTO
    public SkillDTO modelToDto(SkillModel model) {
        if (model == null) return null;

        SkillDTO dto = new SkillDTO();
        dto.setId(model.getId());
        dto.setSkillName(model.getSkillName());
        return dto;
    }

    // Model to Entity
    public SkillEntity modelToEntity(SkillModel model) {
        if (model == null) return null;

        SkillEntity entity = new SkillEntity();
        entity.setSkillId(model.getId());
        entity.setSkillName(model.getSkillName());
        return entity;
    }

    // Entity to Model
    public SkillModel entityToModel(SkillEntity entity) {
        if (entity == null) return null;

        SkillModel model = new SkillModel();
        model.setId(entity.getSkillId());
        model.setSkillName(entity.getSkillName());
        return model;
    }

    // List conversions
    public List<SkillModel> dtosToModels(List<SkillDTO> dtos) {
        if (dtos == null) return null;
        return dtos.stream().map(this::dtoToModel).collect(Collectors.toList());
    }

    public List<SkillDTO> modelsToDtos(List<SkillModel> models) {
        if (models == null) return null;
        return models.stream().map(this::modelToDto).collect(Collectors.toList());
    }

    public List<SkillEntity> modelsToEntities(List<SkillModel> models) {
        if (models == null) return null;
        return models.stream().map(this::modelToEntity).collect(Collectors.toList());
    }

    public List<SkillModel> entitiesToModels(List<SkillEntity> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::entityToModel).collect(Collectors.toList());
    }
}
