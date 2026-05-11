package com.AuraHealth.api.auracontrollers;

import com.aurahealth.api.auradtos.MedicationRequestDTO;
import com.aurahealth.api.auradtos.MedicationResponseDTO;
import com.aurahealth.api.auraservices.MedicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "EP03 · Medication Management",
     description = "HU08-HU10 — Registro, seguimiento diario y eliminación de medicamentos")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    // ── HU08 — Registrar ─────────────────────────────────────────────────────

    @Operation(summary = "HU08 — Registrar nuevo medicamento",
               description = "Valida duplicidad por nombre (case-insensitive). Si existe → 409.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Medicamento registrado."),
        @ApiResponse(responseCode = "400", description = "Nombre obligatorio u otros datos inválidos."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado."),
        @ApiResponse(responseCode = "409", description = "Medicamento duplicado.")
    })
    @PostMapping("/users/{userId}/medications")
    public ResponseEntity<MedicationResponseDTO> createMedication(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody MedicationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicationService.crear(userId, dto));
    }

    // ── HU09 — Listar ────────────────────────────────────────────────────────

    @Operation(summary = "HU09 — Listar medicamentos del usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista obtenida (puede ser vacía)."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @GetMapping("/users/{userId}/medications")
    public ResponseEntity<List<MedicationResponseDTO>> listMedications(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long userId) {
        return ResponseEntity.ok(medicationService.listar(userId));
    }

    // ── HU09 — Detalle ───────────────────────────────────────────────────────

    @Operation(summary = "HU09 — Obtener detalle de un medicamento")
    @GetMapping("/users/{userId}/medications/{id}")
    public ResponseEntity<MedicationResponseDTO> getMedication(
            @PathVariable Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(medicationService.obtenerPorId(userId, id));
    }

    // ── HU08 — Actualizar ────────────────────────────────────────────────────

    @Operation(summary = "HU08 — Actualizar medicamento")
    @PutMapping("/users/{userId}/medications/{id}")
    public ResponseEntity<MedicationResponseDTO> updateMedication(
            @PathVariable Long userId, @PathVariable Long id,
            @Valid @RequestBody MedicationRequestDTO dto) {
        return ResponseEntity.ok(medicationService.actualizar(userId, id, dto));
    }

    // ── HU10 — Eliminar ──────────────────────────────────────────────────────

    @Operation(summary = "HU10 — Eliminar medicamento")
    @ApiResponse(responseCode = "204", description = "Medicamento eliminado.")
    @DeleteMapping("/users/{userId}/medications/{id}")
    public ResponseEntity<Void> deleteMedication(
            @PathVariable Long userId, @PathVariable Long id) {
        medicationService.eliminar(userId, id);
        return ResponseEntity.noContent().build();
    }

    // ── HU09 — Toggle diario ─────────────────────────────────────────────────

    @Operation(summary = "HU09 — Marcar/desmarcar toma de hoy (toggle)",
               description = "Idempotente: primera llamada true→false, segunda false→true.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado.")
    @PatchMapping("/users/{userId}/medications/{id}/toggle-today")
    public ResponseEntity<MedicationResponseDTO> toggleTodayCheckbox(
            @PathVariable Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(medicationService.toggleCompletadoHoy(userId, id));
    }

    // ── HU46 — Toggle privacidad ──────────────────────────────────────────────

    @Operation(summary = "HU46 — Alternar visibilidad con Partner")
    @PatchMapping("/users/{userId}/medications/{id}/toggle-privacy")
    public ResponseEntity<MedicationResponseDTO> togglePartnerVisibility(
            @PathVariable Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(medicationService.togglePrivacidad(userId, id));
    }
}