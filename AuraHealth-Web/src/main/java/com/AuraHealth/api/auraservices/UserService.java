package com.aurahealth.api.auraservices;

import com.aurahealth.api.auraentities.HealthProfile;
import com.aurahealth.api.auraentities.User;
import com.aurahealth.api.aurarepositories.HealthProfileRepository;
import com.aurahealth.api.aurarepositories.UserRepository;
import com.aurahealth.api.auradtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;

@Service
public class UserService {

    // ── Umbrales clínicos ─────────────────────────────────────────────────────
    private static final BigDecimal GLUCOSE_ALERT_THRESHOLD     = new BigDecimal("126");
    private static final BigDecimal CHOLESTEROL_ALERT_THRESHOLD = new BigDecimal("240");
    private static final int        SYSTOLIC_ALERT_THRESHOLD    = 140;
    private static final int        DIASTOLIC_ALERT_THRESHOLD   = 90;

    // ── Rangos IMC (OMS) ──────────────────────────────────────────────────────
    private static final BigDecimal BMI_UNDERWEIGHT    = new BigDecimal("18.5");
    private static final BigDecimal BMI_NORMAL_MAX     = new BigDecimal("24.9");
    private static final BigDecimal BMI_OVERWEIGHT_MAX = new BigDecimal("29.9");

    // ── Rangos fisiológicos ───────────────────────────────────────────────────
    private static final BigDecimal MIN_WEIGHT_KG = new BigDecimal("1");
    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("500");
    private static final BigDecimal MIN_HEIGHT_CM = new BigDecimal("30");
    private static final BigDecimal MAX_HEIGHT_CM = new BigDecimal("300");

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("es", "en");

    private final UserRepository          userRepository;
    private final HealthProfileRepository healthProfileRepository;

    public UserService(UserRepository userRepository,
                       HealthProfileRepository healthProfileRepository) {
        this.userRepository          = userRepository;
        this.healthProfileRepository = healthProfileRepository;
    }

    // ── HU01 — Registrar usuario ──────────────────────────────────────────────

    @Transactional
    public UserResponseDTO registrarUsuario(UserRegistrationRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El correo '" + dto.getEmail() + "' ya está registrado");
        }

        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail().toLowerCase().strip());
        user.setPasswordHash(dto.getPassword()); // En producción: BCrypt
        user.setGender(dto.getGender());
        user.setPreferredLanguage(
            dto.getPreferredLanguage() != null ? dto.getPreferredLanguage() : "es");

        if (dto.getBirthDate() != null && !dto.getBirthDate().isBlank()) {
            user.setBirthDate(LocalDate.parse(dto.getBirthDate()));
        }

        return toUserDto(userRepository.save(user));
    }

    // ── HU02 — Login ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponseDTO loginUsuario(UserLoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail().toLowerCase().strip())
            .filter(u -> u.getPasswordHash().equals(dto.getPassword()))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));
        return toUserDto(user);
    }


}
