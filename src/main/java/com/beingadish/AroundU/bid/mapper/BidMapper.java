package com.beingadish.AroundU.bid.mapper;

import com.beingadish.AroundU.bid.dto.BidCreateRequest;
import com.beingadish.AroundU.bid.dto.BidResponseDTO;
import com.beingadish.AroundU.bid.entity.Bid;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.user.entity.Worker;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BidMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", source = "job")
    @Mapping(target = "worker", source = "worker")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Bid toEntity(BidCreateRequest request, Job job, Worker worker);

    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "workerId", source = "worker.id")
    BidResponseDTO toDto(Bid bid);

    List<BidResponseDTO> toDtoList(List<Bid> bids);
}
