package com.beingadish.AroundU.common.mapper;

import com.beingadish.AroundU.common.dto.AddressDTO;
import com.beingadish.AroundU.location.entity.Address;
import com.beingadish.AroundU.common.model.AddressModel;
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