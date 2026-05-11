package com.AuraHealth.api.auracontrollers;

import com.AuraHealth.api.auradtos.EducationalResourceRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/resources")
@Tag(name = "Z-Admin · Content Management",
     description = "Endpoints restringidos para la gestión administrativa de la biblioteca.")
public class AdminEducationalResourceController {

    @Operation(summary = "Endpoint administrativo - No visible para el paciente")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> placeholder() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}