package com.beingadish.AroundU.common.mapper;

import com.beingadish.AroundU.common.dto.SkillDTO;
import com.beingadish.AroundU.common.entity.Skill;
import com.beingadish.AroundU.common.model.SkillModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface SkillMapper {

    @Mapping(target = "name", source = "skillName")
    @Mapping(target = "jobs", ignore = true)
    Skill toEntity(SkillDTO dto);

    @Mapping(target = "skillName", source = "name")
    SkillDTO toDto(Skill entity);

    @Mapping(target = "skillName", source = "name")
    SkillModel toModel(Skill entity);

    List<SkillDTO> toDtoList(Set<Skill> entities);

    List<SkillModel> toModelList(Set<Skill> entities);

    List<SkillDTO> toDtoListFromList(List<Skill> entities);

    List<SkillModel> toModelListFromList(List<Skill> entities);
}