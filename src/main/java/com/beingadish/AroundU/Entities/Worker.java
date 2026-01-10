package com.beingadish.AroundU.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "workers")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Worker extends User {

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
