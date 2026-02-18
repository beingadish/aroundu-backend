package com.beingadish.AroundU.Entities;

import com.beingadish.AroundU.Constants.Enums.JobStatus;
import com.beingadish.AroundU.Constants.Enums.JobUrgency;
import com.beingadish.AroundU.Constants.Enums.PaymentMode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String title;

    @Column(length = 200)
    @Size(max = 200)
    private String shortDescription;

    @Lob
    @Column(nullable = false)
    private String longDescription;

    @Embedded
    private Price price;

    @ManyToOne
    @JoinColumn(name = "job_location_id", nullable = false)
    private Address jobLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus jobStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobUrgency jobUrgency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentMode paymentMode = PaymentMode.ESCROW;

    @ManyToMany
    @JoinTable(name = "job_required_skills", joinColumns = @JoinColumn(name = "job_id"), inverseJoinColumns = @JoinColumn(name = "skill_id"))
    @Builder.Default
    private Set<Skill> skillSet = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private Client createdBy;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private Worker assignedTo;

    @Column
    private LocalDateTime scheduledStartTime;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
