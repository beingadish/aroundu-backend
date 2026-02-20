package com.beingadish.AroundU.user.mapper;

import com.beingadish.AroundU.user.dto.UserSummaryDTO;
import com.beingadish.AroundU.user.entity.User;
import com.beingadish.AroundU.common.mapper.AddressMapper;
import com.beingadish.AroundU.common.mapper.VerificationStatusMapper;
import com.beingadish.AroundU.user.model.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, VerificationStatusMapper.class})
public interface UserMapper {

    UserSummaryDTO toSummary(User entity);

    @Mapping(target = "hashedPassword", source = "hashedPassword")
    UserModel toModel(User entity);
}