package com.beingadish.AroundU.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double rating;

    @Size(max = 1200)
    private String reviewComment; // Max 1200 Chars

    @ManyToOne
    private Worker worker;
}
