package com.beingadish.AroundU.Entities;

import com.beingadish.AroundU.Constants.Enums.Currency;
import com.beingadish.AroundU.Constants.Enums.JobPriority;
import com.beingadish.AroundU.Constants.Enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long jobId;

    @Column(nullable = false)
    private String jobTitle;

    @Column(nullable = false)
    private String jobDescription;

    private Integer estimatedPrice;

    @Enumerated(EnumType.STRING)
    private Currency currencySymbol;

    @Enumerated(EnumType.STRING)
    private JobPriority jobPriority;

    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private ClientEntity createdBy;

    @ManyToOne
    @JoinColumn(name = "assigned_worker")
    private WorkerEntity assignedWorker;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "job_skills",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<SkillEntity> skillRequiredList;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
