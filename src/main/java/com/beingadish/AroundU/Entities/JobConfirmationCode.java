package com.beingadish.AroundU.Entities;

import com.beingadish.AroundU.Constants.Enums.JobCodeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_confirmation_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobConfirmationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "job_id", unique = true)
    private Job job;

    @Column(length = 20, nullable = false)
    private String startCode;

    @Column(length = 20, nullable = false)
    private String releaseCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobCodeStatus status = JobCodeStatus.START_PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
