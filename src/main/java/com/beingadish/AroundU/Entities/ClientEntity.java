package com.beingadish.AroundU.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.stereotype.Component;

@Entity
@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@EqualsAndHashCode
@Builder
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long clientId;

    @NonNull
    private String clientName;

    @NonNull
    private String clientEmail;

    @NonNull
    private String password;

}
