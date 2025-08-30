package com.beingadish.AroundU.Mappers;

import com.beingadish.AroundU.DTO.Common.AddressDTO;
import com.beingadish.AroundU.Entities.Address;
import com.beingadish.AroundU.Models.AddressModel;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    // DTO to Model
    public AddressModel dtoToModel(AddressDTO dto) {
        if (dto == null) return null;

        AddressModel model = new AddressModel();
        model.setId(dto.getId());
        model.setCountry(dto.getCountry());
        model.setPostalCode(dto.getPostalCode());
        model.setFullAddress(dto.getFullAddress());
        return model;
    }

    // Model to DTO
    public AddressDTO modelToDto(AddressModel model) {
        if (model == null) return null;

        AddressDTO dto = new AddressDTO();
        dto.setId(model.getId());
        dto.setCountry(model.getCountry());
        dto.setPostalCode(model.getPostalCode());
        dto.setFullAddress(model.getFullAddress());
        return dto;
    }

    // Model to Entity
    public Address modelToEntity(AddressModel model) {
        if (model == null) return null;

        Address entity = new Address();
        entity.setAddressId(model.getId());
        entity.setCountry(model.getCountry());
        entity.setPostalCode(model.getPostalCode());
        entity.setFullAddress(model.getFullAddress());
        return entity;
    }

    // Entity to Model
    public AddressModel entityToModel(Address entity) {
        if (entity == null) return null;

        AddressModel model = new AddressModel();
        model.setId(entity.getAddressId());
        model.setCountry(entity.getCountry());
        model.setPostalCode(entity.getPostalCode());
        model.setFullAddress(entity.getFullAddress());
        if (entity.getUser() != null) {
            model.setUserId(entity.getUser().getId());
        }
        return model;
    }

    // DTO to Entity (direct conversion)
    public Address dtoToEntity(AddressDTO dto) {
        if (dto == null) return null;

        Address entity = new Address();
        entity.setAddressId(dto.getId());
        entity.setCountry(dto.getCountry());
        entity.setPostalCode(dto.getPostalCode());
        entity.setFullAddress(dto.getFullAddress());
        return entity;
    }

    // Entity to DTO (direct conversion)
    public AddressDTO entityToDto(Address entity) {
        if (entity == null) return null;

        AddressDTO dto = new AddressDTO();
        dto.setId(entity.getAddressId());
        dto.setCountry(entity.getCountry());
        dto.setPostalCode(entity.getPostalCode());
        dto.setFullAddress(entity.getFullAddress());
        return dto;
    }
}
