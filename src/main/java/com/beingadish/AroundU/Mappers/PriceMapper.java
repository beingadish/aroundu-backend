package com.beingadish.AroundU.Mappers;

import com.beingadish.AroundU.DTO.Common.PriceDTO;
import com.beingadish.AroundU.Entities.Price;
import com.beingadish.AroundU.Models.PriceModel;
import org.springframework.stereotype.Component;

@Component
public class PriceMapper {

    // DTO to Model
    public PriceModel dtoToModel(PriceDTO dto) {
        if (dto == null) return null;

        PriceModel model = new PriceModel();
        model.setCurrency(dto.getCurrency());
        model.setAmount(dto.getAmount());
        return model;
    }

    // Model to DTO
    public PriceDTO modelToDto(PriceModel model) {
        if (model == null) return null;

        PriceDTO dto = new PriceDTO();
        dto.setCurrency(model.getCurrency());
        dto.setAmount(model.getAmount());
        return dto;
    }

    // Model to Entity
    public Price modelToEntity(PriceModel model) {
        if (model == null) return null;

        Price entity = new Price();
        entity.setCurrency(model.getCurrency());
        entity.setAmount(model.getAmount());
        return entity;
    }

    // Entity to Model
    public PriceModel entityToModel(Price entity) {
        if (entity == null) return null;

        PriceModel model = new PriceModel();
        model.setCurrency(entity.getCurrency());
        model.setAmount(entity.getAmount());
        return model;
    }

    // DTO to Entity (direct conversion)
    public Price dtoToEntity(PriceDTO dto) {
        if (dto == null) return null;

        Price entity = new Price();
        entity.setCurrency(dto.getCurrency());
        entity.setAmount(dto.getAmount());
        return entity;
    }

    // Entity to DTO (direct conversion)
    public PriceDTO entityToDto(Price entity) {
        if (entity == null) return null;

        PriceDTO dto = new PriceDTO();
        dto.setCurrency(entity.getCurrency());
        dto.setAmount(entity.getAmount());
        return dto;
    }
}
