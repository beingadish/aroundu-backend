package com.beingadish.AroundU.Mappers.Job;

import com.beingadish.AroundU.Constants.Enums.JobCodeStatus;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.JobConfirmationCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobConfirmationCodeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", source = "job")
    @Mapping(target = "status", expression = "java(com.beingadish.AroundU.Constants.Enums.JobCodeStatus.START_PENDING)")
    JobConfirmationCode create(Job job, String startCode, String releaseCode);
}
