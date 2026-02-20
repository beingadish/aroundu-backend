package com.beingadish.AroundU.common.mapper;

import com.beingadish.AroundU.common.dto.PriceDTO;
import com.beingadish.AroundU.common.entity.Price;
import com.beingadish.AroundU.common.model.PriceModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PriceMapper {

    Price toEntity(PriceDTO dto);

    PriceDTO toDto(Price entity);

    PriceModel toModel(Price entity);

    Price fromModel(PriceModel model);
}