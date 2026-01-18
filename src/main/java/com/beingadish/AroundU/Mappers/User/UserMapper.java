package com.beingadish.AroundU.Mappers.User;

import com.beingadish.AroundU.DTO.User.UserSummaryDTO;
import com.beingadish.AroundU.Entities.User;
import com.beingadish.AroundU.Mappers.Common.AddressMapper;
import com.beingadish.AroundU.Mappers.Common.VerificationStatusMapper;
import com.beingadish.AroundU.Models.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, VerificationStatusMapper.class})
public interface UserMapper {

    UserSummaryDTO toSummary(User entity);

    @Mapping(target = "hashedPassword", source = "hashedPassword")
    UserModel toModel(User entity);
}