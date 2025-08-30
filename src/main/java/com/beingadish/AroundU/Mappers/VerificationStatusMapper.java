package com.beingadish.AroundU.Mappers;

import com.beingadish.AroundU.DTO.Common.VerificationStatusDTO;
import com.beingadish.AroundU.Entities.VerificationStatus;
import com.beingadish.AroundU.Models.VerificationStatusModel;
import org.springframework.stereotype.Component;

@Component
public class VerificationStatusMapper {

    // DTO to Model
    public VerificationStatusModel dtoToModel(VerificationStatusDTO dto) {
        if (dto == null) return null;

        VerificationStatusModel model = new VerificationStatusModel();
        model.setIsVerified(dto.getIsVerified());
        model.setVerifiedAt(dto.getVerifiedAt());
        model.setExpiryDate(dto.getExpiryDate());
        model.setUpdatedAt(dto.getUpdatedAt());
        return model;
    }

    // Model to DTO
    public VerificationStatusDTO modelToDto(VerificationStatusModel model) {
        if (model == null) return null;

        VerificationStatusDTO dto = new VerificationStatusDTO();
        dto.setIsVerified(model.getIsVerified());
        dto.setVerifiedAt(model.getVerifiedAt());
        dto.setExpiryDate(model.getExpiryDate());
        dto.setUpdatedAt(model.getUpdatedAt());
        return dto;
    }

    // Model to Entity
    public VerificationStatus modelToEntity(VerificationStatusModel model) {
        if (model == null) return null;

        VerificationStatus entity = new VerificationStatus();
        entity.setIsVerified(model.getIsVerified());
        entity.setVerifiedAt(model.getVerifiedAt());
        entity.setExpiryDate(model.getExpiryDate());
        entity.setUpdatedAt(model.getUpdatedAt());
        return entity;
    }

    // Entity to Model
    public VerificationStatusModel entityToModel(VerificationStatus entity) {
        if (entity == null) return null;

        VerificationStatusModel model = new VerificationStatusModel();
        model.setIsVerified(entity.getIsVerified());
        model.setVerifiedAt(entity.getVerifiedAt());
        model.setExpiryDate(entity.getExpiryDate());
        model.setUpdatedAt(entity.getUpdatedAt());
        return model;
    }
}
