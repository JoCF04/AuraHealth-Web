package com.aurahealth.api.aurarepositories;

import com.aurahealth.api.auraentities.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {

    // HU09 — listar medicamentos del usuario ordenados por nombre
    List<Medication> findByUserIdOrderByNameAsc(Long userId);

    // Ownership check: el medicamento debe pertenecer al usuario
    Optional<Medication> findByIdAndUserId(Long id, Long userId);

    // HU08 — verificar duplicado por nombre exacto (ignorando mayúsculas)
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    // HU08 — verificar duplicado excluyendo el propio registro (para PUT)
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long excludeId);
}
