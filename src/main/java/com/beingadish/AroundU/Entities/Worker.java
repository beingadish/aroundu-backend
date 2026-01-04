package com.beingadish.AroundU.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "workers")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worker extends User {

    @OneToMany(mappedBy = "assignedTo")
    private List<Job> engagedJobList;

    private Double overallRating;

    @OneToMany(mappedBy = "worker")
    private List<Review> reviews;
}
