package com.beingadish.AroundU.user.mapper;

import com.beingadish.AroundU.common.mapper.AddressMapper;
import com.beingadish.AroundU.common.mapper.VerificationStatusMapper;
import com.beingadish.AroundU.user.dto.client.ClientDetailsResponseDTO;
import com.beingadish.AroundU.user.dto.client.ClientRegisterRequestDTO;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.model.ClientModel;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, VerificationStatusMapper.class}, builder = @Builder(disableBuilder = true), unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "savedAddresses", ignore = true)
    ClientRegisterRequestDTO modelToRegisterRequestDto(ClientModel model);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "hashedPassword", ignore = true)
    @Mapping(target = "profileImageUrl", ignore = true)
    @Mapping(target = "postedJobIds", ignore = true)
    @Mapping(target = "savedAddressIds", ignore = true)
    ClientModel registerRequestDtoToModel(ClientRegisterRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postedJobs", ignore = true)
    @Mapping(target = "savedAddresses", ignore = true)
    @Mapping(target = "currentAddress", source = "currentAddress")
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Client modelToEntity(ClientModel model);

    @Mapping(
            target = "postedJobIds",
            expression = "java(entity.getPostedJobs() == null ? java.util.List.of() : entity.getPostedJobs().stream().map(com.beingadish.AroundU.job.entity.Job::getId).toList())"
    )
    @Mapping(
            target = "savedAddressIds",
            expression = "java(entity.getSavedAddresses() == null ? java.util.List.of() : entity.getSavedAddresses().stream().map(com.beingadish.AroundU.location.entity.Address::getAddressId).toList())"
    )
    ClientModel entityToModel(Client entity);

    ClientDetailsResponseDTO modelToClientDetailsResponseDto(ClientModel model);

    List<ClientDetailsResponseDTO> modelListToClientDetailsResponseDtoList(List<ClientModel> models);
}