package com.beingadish.AroundU.Mappers.Common;

import com.beingadish.AroundU.DTO.Common.SkillDTO;
import com.beingadish.AroundU.Entities.Skill;
import com.beingadish.AroundU.Models.SkillModel;
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