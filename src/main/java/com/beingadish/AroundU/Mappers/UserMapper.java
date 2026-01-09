package com.beingadish.AroundU.Mappers;

import com.beingadish.AroundU.DTO.User.UserDetailDTO;
import com.beingadish.AroundU.DTO.User.UserSummaryDTO;
import com.beingadish.AroundU.Entities.User;
import com.beingadish.AroundU.Models.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private VerificationStatusMapper verificationStatusMapper;

    // UserDetailDTO to Model
    public UserModel userDetailDtoToModel(UserDetailDTO dto) {
        if (dto == null) return null;

        UserModel model = new UserModel();
        model.setId(dto.getId());
        model.setName(dto.getName());
        model.setEmail(dto.getEmail());
        model.setPhoneNumber(dto.getPhoneNumber());
        model.setCurrency(dto.getCurrency());
        model.setCurrentAddress(addressMapper.dtoToModel(dto.getCurrentAddress()));
        model.setVerificationStatus(verificationStatusMapper.dtoToModel(dto.getVerificationStatus()));
        return model;
    }

    // Model to UserDetailDTO
    public UserDetailDTO modelToUserDetailDto(UserModel model) {
        if (model == null) return null;

        UserDetailDTO dto = new UserDetailDTO();
        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setEmail(model.getEmail());
        dto.setPhoneNumber(model.getPhoneNumber());
        dto.setCurrency(model.getCurrency());
        dto.setCurrentAddress(addressMapper.modelToDto(model.getCurrentAddress()));
        dto.setVerificationStatus(verificationStatusMapper.modelToDto(model.getVerificationStatus()));
        return dto;
    }

    // UserSummaryDTO to Model
    public UserModel userSummaryDtoToModel(UserSummaryDTO dto) {
        if (dto == null) return null;

        UserModel model = new UserModel();
        model.setId(dto.getId());
        model.setName(dto.getName());
        model.setEmail(dto.getEmail());
        model.setPhoneNumber(dto.getPhoneNumber());
        model.setCurrency(dto.getCurrency());
        return model;
    }

    // Model to UserSummaryDTO
    public UserSummaryDTO modelToUserSummaryDto(UserModel model) {
        if (model == null) return null;

        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setEmail(model.getEmail());
        dto.setPhoneNumber(model.getPhoneNumber());
        dto.setCurrency(model.getCurrency());
        return dto;
    }

    // Entity to Model (base conversion - will be extended by ClientMapper and WorkerMapper)
    public UserModel entityToModel(User entity) {
        if (entity == null) return null;

        UserModel model = new UserModel();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setEmail(entity.getEmail());
        model.setPhoneNumber(entity.getPhoneNumber());
        model.setCurrency(entity.getCurrency());
        model.setCurrentAddress(addressMapper.entityToModel(entity.getCurrentAddress()));
        model.setVerificationStatus(verificationStatusMapper.entityToModel(entity.getVerificationStatus()));
        return model;
    }

    // Model to Entity (base conversion)
    public void mapModelToEntity(UserModel model, User entity) {
        if (model == null || entity == null) return;

        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setEmail(model.getEmail());
        entity.setPhoneNumber(model.getPhoneNumber());
        entity.setCurrency(model.getCurrency());
        entity.setHashedPassword(model.getHashedPassword());
        entity.setCurrentAddress(addressMapper.modelToEntity(model.getCurrentAddress()));
        entity.setVerificationStatus(verificationStatusMapper.modelToEntity(model.getVerificationStatus()));
    }
}
