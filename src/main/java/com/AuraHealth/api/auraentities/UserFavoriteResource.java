package com.AuraHealth.api.auraentities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_favorite_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFavoriteResource {

    @EmbeddedId
    private UserFavoriteResourceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("resourceId")
    @JoinColumn(name = "resource_id")
    private EducationalResource resource;

    @Column(name = "saved_at", updatable = false)
    private LocalDateTime savedAt;

    @PrePersist
    protected void onPersist() {
        this.savedAt = LocalDateTime.now();
    }
}