package com.AuraHealth.api.auracontrollers;

import com.AuraHealth.api.auradtos.*;
import com.AuraHealth.api.auraservices.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "EP04 · Reminders & Appointments",
     description = "HU11-HU14, HU25-HU26 — Recordatorios médicos y citas")
@PreAuthorize("hasRole('USER')")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    // ══ RECORDATORIOS ═════════════════════════════════════════════════════════

    @Operation(summary = "HU12 — Crear recordatorio (fecha debe ser futura)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Recordatorio creado."),
        @ApiResponse(responseCode = "400", description = "Fecha pasada, tipo inválido o datos faltantes."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @PostMapping("/users/{userId}/reminders")
    public ResponseEntity<ReminderResponseDTO> createReminder(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody ReminderRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reminderService.crearRecordatorio(userId, dto));
    }

    @Operation(summary = "HU11/HU13 — Listar recordatorios (filtro opcional por tipo)")
    @GetMapping("/users/{userId}/reminders")
    public ResponseEntity<List<ReminderResponseDTO>> listReminders(
            @PathVariable Long userId,
            @Parameter(description = "medical | medicine | exam | vaccine")
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(reminderService.listarRecordatorios(userId, type));
    }

    @Operation(summary = "HU11 — Listar recordatorios pendientes (isDone=false, fecha ≥ hoy)")
    @GetMapping("/users/{userId}/reminders/pending")
    public ResponseEntity<List<ReminderResponseDTO>> listPendingReminders(
            @PathVariable Long userId) {
        return ResponseEntity.ok(reminderService.listarPendientes(userId));
    }

    @Operation(summary = "HU14 — Listar recordatorios vencidos (isDone=false, fecha < hoy)")
    @GetMapping("/users/{userId}/reminders/overdue")
    public ResponseEntity<List<ReminderResponseDTO>> listOverdueReminders(
            @PathVariable Long userId) {
        return ResponseEntity.ok(reminderService.listarVencidos(userId));
    }

    @Operation(summary = "HU11 — Obtener detalle de un recordatorio")
    @GetMapping("/users/{userId}/reminders/{id}")
    public ResponseEntity<ReminderResponseDTO> getReminder(
            @PathVariable Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(reminderService.obtenerRecordatorioPorId(userId, id));
    }

    @Operation(summary = "HU12 — Actualizar recordatorio")
    @PutMapping("/users/{userId}/reminders/{id}")
    public ResponseEntity<ReminderResponseDTO> updateReminder(
            @PathVariable Long userId, @PathVariable Long id,
            @Valid @RequestBody ReminderRequestDTO dto) {
        return ResponseEntity.ok(reminderService.actualizarRecordatorio(userId, id, dto));
    }

    @Operation(summary = "HU14 — Marcar recordatorio como completado",
               description = "409 si ya estaba completado (idempotency guard).")
    @PatchMapping("/users/{userId}/reminders/{id}/complete")
    public ResponseEntity<ReminderResponseDTO> completeReminder(
            @PathVariable Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(reminderService.marcarComoCompletado(userId, id));
    }

    @Operation(summary = "HU12 — Eliminar recordatorio")
    @DeleteMapping("/users/{userId}/reminders/{id}")
    public ResponseEntity<Void> deleteReminder(
            @PathVariable Long userId, @PathVariable Long id) {
        reminderService.eliminarRecordatorio(userId, id);
        return ResponseEntity.noContent().build();
    }

    // ══ CITAS MÉDICAS ═════════════════════════════════════════════════════════

    @Operation(summary = "HU25 — Agendar cita médica (fecha futura obligatoria)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cita agendada."),
        @ApiResponse(responseCode = "400", description = "Fecha pasada o datos faltantes."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @PostMapping("/users/{userId}/appointments")
    public ResponseEntity<AppointmentResponseDTO> scheduleAppointment(
            @PathVariable Long userId,
            @Valid @RequestBody AppointmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reminderService.agendarCita(userId, dto));
    }

    @Operation(summary = "HU26 — Listar todas las citas del usuario")
    @GetMapping("/users/{userId}/appointments")
    public ResponseEntity<List<AppointmentResponseDTO>> listAppointments(
            @PathVariable Long userId) {
        return ResponseEntity.ok(reminderService.listarCitas(userId));
    }

    @Operation(summary = "HU26 — Próximas citas (fecha ≥ hoy)")
    @GetMapping("/users/{userId}/appointments/upcoming")
    public ResponseEntity<List<AppointmentResponseDTO>> listUpcomingAppointments(
            @PathVariable Long userId) {
        return ResponseEntity.ok(reminderService.listarProximasCitas(userId));
    }

    @Operation(summary = "HU26 — Detalle de una cita")
    @GetMapping("/users/{userId}/appointments/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointment(
            @PathVariable Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(reminderService.obtenerCitaPorId(userId, id));
    }

    @Operation(summary = "HU25 — Actualizar cita médica")
    @PutMapping("/users/{userId}/appointments/{id}")
    public ResponseEntity<AppointmentResponseDTO> updateAppointment(
            @PathVariable Long userId, @PathVariable Long id,
            @Valid @RequestBody AppointmentRequestDTO dto) {
        return ResponseEntity.ok(reminderService.actualizarCita(userId, id, dto));
    }

    @Operation(summary = "HU25 — Cancelar cita médica")
    @DeleteMapping("/users/{userId}/appointments/{id}")
    public ResponseEntity<Void> cancelAppointment(
            @PathVariable Long userId, @PathVariable Long id) {
        reminderService.cancelarCita(userId, id);
        return ResponseEntity.noContent().build();
    }
}