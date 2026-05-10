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

}
