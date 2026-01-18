package com.beingadish.AroundU.Mappers.Common;

import com.beingadish.AroundU.DTO.Common.AddressDTO;
import com.beingadish.AroundU.Entities.Address;
import com.beingadish.AroundU.Models.AddressModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "addressId", source = "id")
    @Mapping(target = "client", ignore = true)
    Address toEntity(AddressDTO dto);

    @Mapping(target = "id", source = "addressId")
    AddressDTO toDto(Address entity);

    @Mapping(target = "id", source = "addressId")
    @Mapping(target = "userId", expression = "java(entity.getClient() != null ? entity.getClient().getId() : null)")
    AddressModel toModel(Address entity);

    @Mapping(target = "addressId", source = "id")
    @Mapping(target = "client", ignore = true)
    Address fromModel(AddressModel model);

    List<AddressDTO> toDtoList(List<Address> entities);

    List<AddressModel> toModelList(List<Address> entities);
}