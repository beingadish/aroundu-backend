package com.beingadish.AroundU.Mappers.User.Client;

import com.beingadish.AroundU.DTO.Client.Details.ClientDetailsResponseDTO;
import com.beingadish.AroundU.DTO.Client.Register.ClientRegisterRequestDTO;
import com.beingadish.AroundU.Entities.Address;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Mappers.Common.AddressMapper;
import com.beingadish.AroundU.Models.ClientModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface ClientMapper {

    ClientRegisterRequestDTO modelToRegisterRequestDto(ClientModel model);

    ClientModel registerRequestDtoToModel(ClientRegisterRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postedJobs", ignore = true)
    @Mapping(target = "savedAddresses", source = "savedAddresses")
    @Mapping(target = "currentAddress", source = "currentAddress")
    @Mapping(target = "verificationStatus", ignore = true)
    Client modelToEntity(ClientModel model);

    @Mapping(
            target = "postedJobIds",
            expression = "java(entity.getPostedJobs() == null ? java.util.List.of() : entity.getPostedJobs().stream().map(com.beingadish.AroundU.Entities.Job::getId).toList())"
    )
    @Mapping(
            target = "savedAddressIds",
            expression = "java(entity.getSavedAddresses() == null ? java.util.List.of() : entity.getSavedAddresses().stream().map(Address::getAddressId).toList())"
    )
    ClientModel entityToModel(Client entity);

    ClientDetailsResponseDTO modelToClientDetailsResponseDto(ClientModel model);

    List<ClientDetailsResponseDTO> modelListToClientDetailsResponseDtoList(List<ClientModel> models);
}