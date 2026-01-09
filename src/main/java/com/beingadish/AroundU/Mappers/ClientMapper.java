package com.beingadish.AroundU.Mappers;

import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterResponseDTO;
import com.beingadish.AroundU.DTO.Common.AddressDTO;
import com.beingadish.AroundU.DTO.Common.VerificationStatusDTO;
import com.beingadish.AroundU.Entities.Client;
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
    public ClientModel registerRequestDtoToModel(ClientRegisterRequestDTO dto) {
        if (dto == null) return null;

        ClientModel model = new ClientModel();
        model.setName(dto.getName());
        model.setEmail(dto.getEmail());
        model.setPhoneNumber(dto.getPhoneNumber());
        model.setCurrency(dto.getCurrency());
        model.setCurrentAddress(addressMapper.dtoToModel(dto.getCurrentAddress()));

        if (dto.getSavedAddresses() != null) {
            model.setSavedAddressIds(dto.getSavedAddresses().stream().map(address -> address.getId()).collect(Collectors.toList()));
        }

        return model;
    }

    // Model to ClientDetailsResponseDto
    public ClientDetailsResponseDTO modelToClientDetailsResponseDto(ClientModel model) {
        if (model == null) return null;

        return ClientDetailsResponseDTO.builder().id(model.getId()).name(model.getName()).currency(model.getCurrency()).phoneNumber(model.getPhoneNumber()).currentAddress(AddressDTO.builder().id(model.getCurrentAddress().getId()).fullAddress(model.getCurrentAddress().getFullAddress()).postalCode(model.getCurrentAddress().getPostalCode()).country(model.getCurrentAddress().getCountry()).build()).verificationStatus(VerificationStatusDTO.builder().expiryDate(model.getVerificationStatus().getExpiryDate()).isVerified(model.getVerificationStatus().getIsVerified()).updatedAt(model.getVerificationStatus().getUpdatedAt()).verifiedAt(model.getVerificationStatus().getVerifiedAt()).build()).build();
    }

    // Entity to Model
    public ClientModel entityToModel(Client entity) {
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
            model.setPostedJobIds(entity.getPostedJobs().stream().map(job -> job.getId()).collect(Collectors.toList()));
        }

        if (entity.getSavedAddresses() != null) {
            model.setSavedAddressIds(entity.getSavedAddresses().stream().map(address -> address.getAddressId()).collect(Collectors.toList()));
        }

        return model;
    }

    // Model to Entity
    public Client modelToEntity(ClientModel model) {
        if (model == null) return null;

        Client entity = new Client();
        // Copy base user fields
        userMapper.mapModelToEntity(model, entity);

        // Note: postedJobs and savedAddresses collections will need to be resolved in service layer

        return entity;
    }
}
