package com.beingadish.AroundU.common.mapper;

import com.beingadish.AroundU.common.dto.VerificationStatusDTO;
import com.beingadish.AroundU.common.entity.VerificationStatus;
import com.beingadish.AroundU.common.model.VerificationStatusModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VerificationStatusMapper {

    VerificationStatusDTO toDto(VerificationStatus entity);

    VerificationStatusModel toModel(VerificationStatus entity);

    VerificationStatus fromDto(VerificationStatusDTO dto);

    VerificationStatus fromModel(VerificationStatusModel model);
}