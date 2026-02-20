package com.beingadish.AroundU.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.review.entity.Review;

@Entity
@Table(name = "workers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Worker extends User {

    @Column
    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @OneToMany(mappedBy = "assignedTo")
    private List<Job> engagedJobList;

    private Double overallRating;

    private Integer experienceYears;

    @Column(length = 1000)
    private String certifications;

    @Builder.Default
    private Boolean isOnDuty = false;

    @Column(length = 255)
    private String payoutAccount;

    @OneToMany(mappedBy = "worker")
    private List<Review> reviews;
}
