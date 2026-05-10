package com.aurahealth.api.auracontrollers;

import com.aurahealth.api.auradtos.*;
import com.aurahealth.api.auraservices.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "EP01-EP02 · Users & Health Profiles",
     description = "HU01-HU07 — Registro, login, perfil, signos vitales e IMC")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ── HU01 — Registrar usuario ──────────────────────────────────────────────

    @Operation(summary = "HU01 — Registrar nuevo usuario")
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

    // ── HU02 — Login ──────────────────────────────────────────────────────────

    @Operation(summary = "HU02 — Iniciar sesión")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso."),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas.")
    })
    @PostMapping("/users/login")
    public ResponseEntity<UserResponseDTO> loginUser(
            @Valid @RequestBody UserLoginRequestDTO dto) {
        return ResponseEntity.ok(userService.loginUsuario(dto));
    }

    // ── HU03 — Logout ─────────────────────────────────────────────────────────

    @Operation(summary = "HU03 — Cerrar sesión")
    @PostMapping("/users/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    // ── HU04 — Ver perfil ─────────────────────────────────────────────────────

    @Operation(summary = "HU04 — Obtener perfil completo del usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil obtenido."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponseDTO> getUser(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.obtenerUsuarioPorId(id));
    }

    // ── HU05 — Actualizar perfil de salud ─────────────────────────────────────

    @Operation(summary = "HU05 — Actualizar perfil de salud (IMC centralizado en backend)",
               description = "El backend calcula BMI = peso/(altura²) según OMS. El frontend nunca recalcula.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil actualizado. Incluye bmi y bmiCategory."),
        @ApiResponse(responseCode = "400", description = "Valores fuera de rango fisiológico."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @PutMapping("/users/{userId}/health-profile")
    public ResponseEntity<HealthProfileResponseDTO> updateHealthProfile(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long userId,
            @RequestBody HealthProfileRequestDTO dto) {
        return ResponseEntity.ok(userService.actualizarPerfilDeSalud(userId, dto));
    }


    // ── HU06 — Cambiar idioma ─────────────────────────────────────────────────

    @Operation(summary = "HU06 — Actualizar idioma preferido (es | en)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Idioma actualizado."),
        @ApiResponse(responseCode = "400", description = "Código de idioma inválido."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @PatchMapping("/users/{id}/language")
    public ResponseEntity<UserResponseDTO> updateLanguage(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(description = "Código ISO 639-1: es | en", example = "en", required = true)
            @RequestParam String lang) {
        return ResponseEntity.ok(userService.cambiarIdioma(id, lang));
    }
}
