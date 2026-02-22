package com.beingadish.AroundU.review.mapper;

import com.beingadish.AroundU.review.dto.ReviewCreateRequest;
import com.beingadish.AroundU.review.dto.ReviewResponseDTO;
import com.beingadish.AroundU.review.entity.Review;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", source = "job")
    @Mapping(target = "worker", source = "worker")
    @Mapping(target = "reviewer", source = "reviewer")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Review toEntity(ReviewCreateRequest request, Job job, Worker worker, Client reviewer);

    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "workerId", source = "worker.id")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    @Mapping(target = "reviewerName", source = "reviewer.name")
    ReviewResponseDTO toDto(Review review);

    List<ReviewResponseDTO> toDtoList(List<Review> reviews);
}
