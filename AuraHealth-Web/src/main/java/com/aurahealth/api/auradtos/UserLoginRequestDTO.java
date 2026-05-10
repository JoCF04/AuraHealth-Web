package com.aurahealth.api.auradtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** DTO compartido de login — usado por el AuthController de José (HU02). */
@Data
public class UserLoginRequestDTO {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
