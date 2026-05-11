package com.AuraHealth.api.auracontrollers;

import com.AuraHealth.api.auraentities.Role;
import com.AuraHealth.api.aurarepositories.RoleRepository;
import com.AuraHealth.api.auradtos.RoleResponseDTO;
import com.AuraHealth.api.auradtos.UserResponseDTO;
import com.AuraHealth.api.auraservices.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles · Gestión de Roles",
     description = "Administración de roles del sistema. Solo accesible por ADMIN.")
public class RoleController {

    private final RoleRepository roleRepository;
    private final UserService    userService;

    public RoleController(RoleRepository roleRepository, UserService userService) {
        this.roleRepository = roleRepository;
        this.userService    = userService;
    }

    // ── Listar todos los roles ────────────────────────────────────────────────

    @Operation(summary = "Listar todos los roles del sistema",
               description = "Roles: ADMIN",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de roles."),
        @ApiResponse(responseCode = "401", description = "Token inválido o expirado."),
        @ApiResponse(responseCode = "403", description = "Solo ADMIN.")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<RoleResponseDTO>> listRoles() {
        List<RoleResponseDTO> roles = roleRepository.findAll().stream()
                .map(r -> new RoleResponseDTO(r.getId(), r.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    // ── Crear rol ─────────────────────────────────────────────────────────────

    @Operation(summary = "Crear un nuevo rol",
               description = "Roles: ADMIN — El nombre se guarda como ROLE_NOMBRE en mayúsculas.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rol creado."),
        @ApiResponse(responseCode = "400", description = "Nombre requerido."),
        @ApiResponse(responseCode = "409", description = "El rol ya existe.")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RoleResponseDTO> createRole(
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'name' es requerido.");
        }
        String fullName = "ROLE_" + name.toUpperCase().strip();
        if (roleRepository.findByName(fullName).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El rol '" + fullName + "' ya existe.");
        }
        Role saved = roleRepository.save(new Role(fullName));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RoleResponseDTO(saved.getId(), saved.getName()));
    }

    // ── Eliminar rol ──────────────────────────────────────────────────────────

    @Operation(summary = "Eliminar un rol por ID",
               description = "Roles: ADMIN",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Rol eliminado."),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado.")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado con id: " + id);
        }
        roleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Asignar rol a usuario ─────────────────────────────────────────────────

    @Operation(summary = "Cambiar el rol de un usuario",
               description = "Roles: ADMIN — Enviar en body: {\"role\": \"USER\"} o DOCTOR o ADMIN",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol actualizado."),
        @ApiResponse(responseCode = "400", description = "Rol inválido."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{userId}")
    public ResponseEntity<UserResponseDTO> assignRoleToUser(
            @Parameter(description = "ID del usuario", example = "1", required = true)
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'role' es requerido.");
        }
        return ResponseEntity.ok(userService.cambiarRolDeUsuario(userId, role));
    }
}
