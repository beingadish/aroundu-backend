package com.beingadish.AroundU.Entities;

import lombok.*;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@EqualsAndHashCode
@Builder
public class WorkerEntity {
    private Long workerId;

    @NonNull
    private String workerName;

    @NonNull
    private String workerEmail;

    @NonNull
    private String password;
}
