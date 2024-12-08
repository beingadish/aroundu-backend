package com.beingadish.AroundU.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Coordinate;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "workers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workerId;

    @Column(nullable = false)
    private String workerName;

    @Column(unique = true)
    private String workerEmail;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private Long aadharId;

    @Column(nullable = false)
    private Boolean verified = false;

    private String addressLine1;
    private String addressLine2;
    private Integer pinCode;

    @ManyToOne
    @JoinColumn(name = "district_id")
    private DistrictEntity district;

    @ManyToOne
    @JoinColumn(name = "state_id")
    private StateEntity state;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private CountryEntity country;

    @Column(columnDefinition = "POINT")
    private Coordinate currentLocation;

    @ManyToMany
    @JoinTable(
            name = "worker_jobs",
            joinColumns = @JoinColumn(name = "worker_id"),
            inverseJoinColumns = @JoinColumn(name = "job_id")
    )
    private List<JobEntity> previouslyInteractedJobList;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
