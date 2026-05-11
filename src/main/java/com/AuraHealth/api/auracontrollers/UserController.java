package com.AuraHealth.api.auracontrollers;

import com.AuraHealth.api.auradtos.*;
import com.AuraHealth.api.auraservices.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "EP01-EP02 · Users & Health Profiles",
     description = "HU01-HU07 — Registro, perfil, signos vitales e IMC")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ── HU01 — Registrar usuario (público) ───────────────────────────────────

    @Operation(summary = "HU01 — Registrar nuevo usuario",
               description = "Acceso: PÚBLICO — no requiere token.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente."),
        @ApiResponse(responseCode = "400", description = "Datos inválidos."),
        @ApiResponse(responseCode = "409", description = "El correo ya está registrado.")
    })
    @PostMapping("/users")
    public ResponseEntity<UserResponseDTO> registerUser(
            @Valid @RequestBody UserRegistrationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registrarUsuario(dto));
    }

    // ── HU04 — Ver perfil ─────────────────────────────────────────────────────

    @Operation(summary = "HU04 — Obtener perfil completo del usuario",
               description = "Roles: USER · DOCTOR · ADMIN",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil obtenido."),
        @ApiResponse(responseCode = "401", description = "Token inválido o expirado."),
        @ApiResponse(responseCode = "403", description = "Sin permisos."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @PreAuthorize("hasAnyRole('USER','DOCTOR','ADMIN')")
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponseDTO> getUser(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.obtenerUsuarioPorId(id));
    }

    // ── HU05 — Actualizar perfil de salud ─────────────────────────────────────

    @Operation(summary = "HU05 — Actualizar perfil de salud (IMC centralizado en backend)",
               description = "Roles: USER · DOCTOR · ADMIN — El backend calcula BMI según OMS.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil actualizado. Incluye bmi y bmiCategory."),
        @ApiResponse(responseCode = "400", description = "Valores fuera de rango fisiológico."),
        @ApiResponse(responseCode = "401", description = "Token inválido o expirado."),
        @ApiResponse(responseCode = "403", description = "Sin permisos."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @PreAuthorize("hasAnyRole('USER','DOCTOR','ADMIN')")
    @PutMapping("/users/{userId}/health-profile")
    public ResponseEntity<HealthProfileResponseDTO> updateHealthProfile(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long userId,
            @RequestBody HealthProfileRequestDTO dto) {
        return ResponseEntity.ok(userService.actualizarPerfilDeSalud(userId, dto));
    }

    // ── HU07 — Signos vitales ─────────────────────────────────────────────────

    @Operation(summary = "HU07 — Registrar signos vitales (Motor de Reglas Médicas)",
               description = "Roles: USER · DOCTOR · ADMIN — Evalúa glucosa ≥126, presión ≥140/90, colesterol ≥240.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Signos vitales registrados."),
        @ApiResponse(responseCode = "400", description = "Formato de presión inválido."),
        @ApiResponse(responseCode = "401", description = "Token inválido o expirado."),
        @ApiResponse(responseCode = "403", description = "Sin permisos."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @PreAuthorize("hasAnyRole('USER','DOCTOR','ADMIN')")
    @PatchMapping("/users/{userId}/vital-signs")
    public ResponseEntity<HealthProfileResponseDTO> updateVitalSigns(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long userId,
            @RequestBody VitalSignsRequestDTO dto) {
        return ResponseEntity.ok(userService.registrarSignosVitales(userId, dto));
    }

    // ── HU06 — Cambiar idioma ─────────────────────────────────────────────────

    @Operation(summary = "HU06 — Actualizar idioma preferido (es | en)",
               description = "Roles: USER · ADMIN",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Idioma actualizado."),
        @ApiResponse(responseCode = "400", description = "Código de idioma inválido."),
        @ApiResponse(responseCode = "401", description = "Token inválido o expirado."),
        @ApiResponse(responseCode = "403", description = "Sin permisos."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PatchMapping("/users/{id}/language")
    public ResponseEntity<UserResponseDTO> updateLanguage(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(description = "Código ISO 639-1: es | en", example = "en", required = true)
            @RequestParam String lang) {
        return ResponseEntity.ok(userService.cambiarIdioma(id, lang));
    }
}
