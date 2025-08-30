package com.beingadish.AroundU.Mappers;

import com.beingadish.AroundU.DTO.Client.ClientDetailDTO;
import com.beingadish.AroundU.DTO.Client.ClientSignupRequestDTO;
import com.beingadish.AroundU.Entities.ClientEntity;
import com.beingadish.AroundU.Models.ClientModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ClientMapper {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressMapper addressMapper;

    // ClientSignupRequestDTO to Model
    public ClientModel signupRequestDtoToModel(ClientSignupRequestDTO dto) {
        if (dto == null) return null;

        ClientModel model = new ClientModel();
        model.setName(dto.getName());
        model.setEmail(dto.getEmail());
        model.setPhoneNumber(dto.getPhoneNumber());
        model.setCurrency(dto.getCurrency());
        model.setCurrentAddress(addressMapper.dtoToModel(dto.getCurrentAddress()));

        if (dto.getSavedAddresses() != null) {
            model.setSavedAddressIds(dto.getSavedAddresses().stream()
                    .map(address -> address.getId())
                    .collect(Collectors.toList()));
        }

        return model;
    }

    // ClientDetailDTO to Model
    public ClientModel clientDetailDtoToModel(ClientDetailDTO dto) {
        if (dto == null) return null;

        ClientModel model = new ClientModel();
        // Copy base user fields
        model.setId(dto.getId());
        model.setName(dto.getName());
        model.setEmail(dto.getEmail());
        model.setPhoneNumber(dto.getPhoneNumber());
        model.setCurrency(dto.getCurrency());
        model.setCurrentAddress(addressMapper.dtoToModel(dto.getCurrentAddress()));

        // Client-specific fields
        model.setPostedJobIds(dto.getPostedJobIds());

        if (dto.getSavedAddresses() != null) {
            model.setSavedAddressIds(dto.getSavedAddresses().stream()
                    .map(address -> address.getId())
                    .collect(Collectors.toList()));
        }

        return model;
    }

    // Model to ClientDetailDTO
    public ClientDetailDTO modelToClientDetailDto(ClientModel model) {
        if (model == null) return null;

        ClientDetailDTO dto = new ClientDetailDTO();
        // Copy base user fields
        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setEmail(model.getEmail());
        dto.setPhoneNumber(model.getPhoneNumber());
        dto.setCurrency(model.getCurrency());
        dto.setCurrentAddress(addressMapper.modelToDto(model.getCurrentAddress()));

        // Client-specific fields
        dto.setPostedJobIds(model.getPostedJobIds());
        // Note: savedAddresses will need to be resolved from savedAddressIds in service layer

        return dto;
    }

    // Entity to Model
    public ClientModel entityToModel(ClientEntity entity) {
        if (entity == null) return null;

        ClientModel model = new ClientModel();
        // Copy base user fields
        userMapper.entityToModel(entity);
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setEmail(entity.getEmail());
        model.setPhoneNumber(entity.getPhoneNumber());
        model.setCurrency(entity.getCurrency());
        model.setCurrentAddress(addressMapper.entityToModel(entity.getCurrentAddress()));

        // Client-specific fields
        if (entity.getPostedJobs() != null) {
            model.setPostedJobIds(entity.getPostedJobs().stream()
                    .map(job -> job.getId())
                    .collect(Collectors.toList()));
        }

        if (entity.getSavedAddresses() != null) {
            model.setSavedAddressIds(entity.getSavedAddresses().stream()
                    .map(address -> address.getAddressId())
                    .collect(Collectors.toList()));
        }

        return model;
    }

    // Model to Entity
    public ClientEntity modelToEntity(ClientModel model) {
        if (model == null) return null;

        ClientEntity entity = new ClientEntity();
        // Copy base user fields
        userMapper.mapModelToEntity(model, entity);

        // Note: postedJobs and savedAddresses collections will need to be resolved in service layer

        return entity;
    }
}
