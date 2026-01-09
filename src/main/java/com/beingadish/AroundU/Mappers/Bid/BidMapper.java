package com.beingadish.AroundU.Mappers.Bid;

import com.beingadish.AroundU.DTO.Bid.BidCreateRequest;
import com.beingadish.AroundU.DTO.Bid.BidResponseDTO;
import com.beingadish.AroundU.Entities.Bid;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.Worker;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BidMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", source = "job")
    @Mapping(target = "worker", source = "worker")
    @Mapping(target = "status", constant = "PENDING")
    Bid toEntity(BidCreateRequest request, Job job, Worker worker);

    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "workerId", source = "worker.id")
    BidResponseDTO toDto(Bid bid);

    List<BidResponseDTO> toDtoList(List<Bid> bids);
}
