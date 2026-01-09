package com.beingadish.AroundU.Mappers.Job;

import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.JobConfirmationCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobConfirmationCodeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", source = "job")
    JobConfirmationCode create(Job job, String startCode, String releaseCode);
}
