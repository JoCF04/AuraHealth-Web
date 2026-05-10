package com.aurahealth.api.aurarepositories;

import com.aurahealth.api.auraentities.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Optional<ActivityLog> findByUserIdAndLogDate(Long userId, LocalDate date);
}