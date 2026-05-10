package com.aurahealth.api.auradtos;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthDate;
    private String gender;
    private Boolean isEmailVerified;
    private String preferredLanguage;
    private LocalDateTime createdAt;

    /** Null si el usuario aún no completó su perfil de salud. */
    private HealthProfileResponseDTO healthProfile;
}
