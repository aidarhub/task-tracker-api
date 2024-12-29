package org.example.api.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskStateDto {
    @NonNull
    Long id;

    @NonNull
    String name;

    Long ordinal;

    Instant createAt;
}
