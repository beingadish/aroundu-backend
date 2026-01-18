package com.beingadish.AroundU.Mappers.Common;

import com.beingadish.AroundU.DTO.Common.PriceDTO;
import com.beingadish.AroundU.Entities.Price;
import com.beingadish.AroundU.Models.PriceModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PriceMapper {

    Price toEntity(PriceDTO dto);

    PriceDTO toDto(Price entity);

    PriceModel toModel(Price entity);

    Price fromModel(PriceModel model);
}