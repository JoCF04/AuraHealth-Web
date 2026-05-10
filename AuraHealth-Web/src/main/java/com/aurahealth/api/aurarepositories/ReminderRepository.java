package com.aurahealth.api.aurarepositories;

import com.aurahealth.api.auraentities.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    // HU11 — recordatorios pendientes: no completados con fecha >= hoy
    List<Reminder> findByUserIdAndIsDoneFalseAndScheduledDateGreaterThanEqualOrderByScheduledDateAsc(
            Long userId, LocalDate today);

    // HU12 — todos los recordatorios del usuario ordenados por fecha
    List<Reminder> findByUserIdOrderByScheduledDateAsc(Long userId);

    // Ownership check: el recordatorio debe pertenecer al usuario
    Optional<Reminder> findByIdAndUserId(Long id, Long userId);
}
