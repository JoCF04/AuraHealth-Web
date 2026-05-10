package com.aurahealth.api.auraentities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "gender", length = 50)
    private String gender;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Column(name = "preferred_language", length = 10, nullable = false)
    private String preferredLanguage = "es";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private HealthProfile healthProfile;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isEmailVerified == null) isEmailVerified = false;
        if (preferredLanguage == null) preferredLanguage = "es";
    }
}
