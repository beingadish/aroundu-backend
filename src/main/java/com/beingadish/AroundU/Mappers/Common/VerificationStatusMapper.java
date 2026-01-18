package com.beingadish.AroundU.Mappers.Common;

import com.beingadish.AroundU.DTO.Common.VerificationStatusDTO;
import com.beingadish.AroundU.Entities.VerificationStatus;
import com.beingadish.AroundU.Models.VerificationStatusModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VerificationStatusMapper {

    VerificationStatusDTO toDto(VerificationStatus entity);

    VerificationStatusModel toModel(VerificationStatus entity);

    VerificationStatus fromDto(VerificationStatusDTO dto);

    VerificationStatus fromModel(VerificationStatusModel model);
}