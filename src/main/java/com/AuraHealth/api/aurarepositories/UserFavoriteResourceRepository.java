package com.AuraHealth.api.aurarepositories;

import com.AuraHealth.api.auraentities.UserFavoriteResource;
import com.AuraHealth.api.auraentities.UserFavoriteResourceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavoriteResourceRepository
        extends JpaRepository<UserFavoriteResource, UserFavoriteResourceId> {

    @Query("SELECT ufr FROM UserFavoriteResource ufr " +
           "JOIN FETCH ufr.resource r " +
           "WHERE ufr.id.userId = :userId " +
           "AND r.isPublished = true " +
           "ORDER BY ufr.savedAt DESC")
    List<UserFavoriteResource> findByIdUserIdOrderBySavedAtDesc(@Param("userId") Long userId);

    boolean existsByIdUserIdAndIdResourceId(Long userId, Long resourceId);
}