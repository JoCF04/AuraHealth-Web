package com.AuraHealth.api.auraservices;

import com.AuraHealth.api.auraentities.Role;
import com.AuraHealth.api.auraentities.HealthProfile;
import com.AuraHealth.api.auraentities.User;
import com.AuraHealth.api.aurarepositories.RoleRepository;
import com.AuraHealth.api.aurarepositories.HealthProfileRepository;
import com.AuraHealth.api.aurarepositories.UserRepository;
import com.AuraHealth.api.auradtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
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
    private final RoleRepository          roleRepository;
    private final PasswordEncoder         passwordEncoder;

    public UserService(UserRepository userRepository,
                       HealthProfileRepository healthProfileRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository          = userRepository;
        this.healthProfileRepository = healthProfileRepository;
        this.roleRepository          = roleRepository;
        this.passwordEncoder         = passwordEncoder;
    }

    // ── HU01 — Registrar usuario ──────────────────────────────────────────────

    @Transactional
    public UserResponseDTO registrarUsuario(UserRegistrationRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El correo '" + dto.getEmail() + "' ya está registrado");
        }

        String roleName = "ROLE_" + (dto.getRole() != null ? dto.getRole() : "USER");
        Role userRole = roleRepository.findByName(roleName)
            .orElseGet(() -> roleRepository.save(new Role(roleName)));

        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail().toLowerCase().strip());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setGender(dto.getGender());
        user.setIsEmailVerified(true);
        user.setPreferredLanguage(
            dto.getPreferredLanguage() != null ? dto.getPreferredLanguage() : "es");
        user.setRoles(new HashSet<>(Set.of(userRole)));

        if (dto.getBirthDate() != null && !dto.getBirthDate().isBlank()) {
            user.setBirthDate(LocalDate.parse(dto.getBirthDate()));
        }

        return toUserDto(userRepository.save(user));
    }

    // ── HU04 — Ver perfil ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponseDTO obtenerUsuarioPorId(Long id) {
        User user = userRepository.findByIdWithHealthProfile(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Usuario no encontrado con id: " + id));
        return toUserDto(user);
    }

    // ── HU05 — Actualizar perfil de salud + IMC ───────────────────────────────

    @Transactional
    public HealthProfileResponseDTO actualizarPerfilDeSalud(Long userId,
                                                             HealthProfileRequestDTO dto) {
        User user = requireUser(userId);
        validatePhysiologicalBounds(dto.getWeightKg(), dto.getHeightCm());

        HealthProfile profile = (user.getHealthProfile() != null)
            ? user.getHealthProfile()
            : new HealthProfile();
        profile.setUser(user);

        if (dto.getBloodType()        != null) profile.setBloodType(dto.getBloodType());
        if (dto.getBloodPressure()    != null) profile.setBloodPressure(dto.getBloodPressure());
        if (dto.getGlucoseLevel()     != null) profile.setGlucoseLevel(dto.getGlucoseLevel());
        if (dto.getCholesterolLevel() != null) profile.setCholesterolLevel(dto.getCholesterolLevel());
        if (dto.getAllergies()         != null) profile.setAllergies(dto.getAllergies());
        if (dto.getWeightKg()         != null) profile.setWeightKg(dto.getWeightKg());
        if (dto.getHeightCm()         != null) profile.setHeightCm(dto.getHeightCm());

        recalculateBmi(profile);
        return toHealthProfileDto(healthProfileRepository.save(profile));
    }

    // ── HU07 — Signos vitales + Motor de Reglas Médicas ──────────────────────

    @Transactional
    public HealthProfileResponseDTO registrarSignosVitales(Long userId,
                                                            VitalSignsRequestDTO dto) {
        User user = requireUser(userId);

        HealthProfile profile = (user.getHealthProfile() != null)
            ? user.getHealthProfile()
            : new HealthProfile();
        profile.setUser(user);

        StringBuilder alertMessages = new StringBuilder();
        boolean criticalValueDetected = false;

        if (dto.getGlucoseLevel() != null) {
            profile.setGlucoseLevel(dto.getGlucoseLevel());
            if (dto.getGlucoseLevel().compareTo(GLUCOSE_ALERT_THRESHOLD) >= 0) {
                criticalValueDetected = true;
                alertMessages.append("ALERTA: Glucosa en ayuno ")
                    .append(dto.getGlucoseLevel())
                    .append(" mg/dL ≥ 126 mg/dL (umbral diagnóstico ADA de diabetes). ");
            }
        }

        if (dto.getBloodPressure() != null && !dto.getBloodPressure().isBlank()) {
            profile.setBloodPressure(dto.getBloodPressure());
            int[] bp = parseBloodPressure(dto.getBloodPressure());
            if (bp[0] >= SYSTOLIC_ALERT_THRESHOLD || bp[1] >= DIASTOLIC_ALERT_THRESHOLD) {
                criticalValueDetected = true;
                alertMessages.append("ALERTA: Presión arterial ")
                    .append(dto.getBloodPressure())
                    .append(" mmHg supera el umbral AHA de hipertensión stage 1 (140/90). ");
            }
        }

        if (dto.getCholesterolLevel() != null) {
            profile.setCholesterolLevel(dto.getCholesterolLevel());
            if (dto.getCholesterolLevel().compareTo(CHOLESTEROL_ALERT_THRESHOLD) >= 0) {
                criticalValueDetected = true;
                alertMessages.append("ALERTA: Colesterol total ")
                    .append(dto.getCholesterolLevel())
                    .append(" mg/dL ≥ 240 mg/dL (riesgo cardiovascular elevado, NCEP). ");
            }
        }

        if (dto.getAllergies() != null) {
            profile.setAllergies(dto.getAllergies());
        }

        profile.setVitalAlertFlag(criticalValueDetected);
        profile.setAlertMessage(criticalValueDetected ? alertMessages.toString().strip() : null);

        recalculateBmi(profile);
        return toHealthProfileDto(healthProfileRepository.save(profile));
    }

    // ── HU06 — Cambiar idioma ─────────────────────────────────────────────────

    @Transactional
    public UserResponseDTO cambiarIdioma(Long id, String lang) {
        if (!SUPPORTED_LANGUAGES.contains(lang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Código de idioma inválido: '" + lang + "'. Valores aceptados: " + SUPPORTED_LANGUAGES);
        }
        User user = requireUser(id);
        user.setPreferredLanguage(lang);
        return toUserDto(userRepository.save(user));
    }

    // ── Cambiar rol de usuario (admin) ────────────────────────────────────────

    @Transactional
    public UserResponseDTO cambiarRolDeUsuario(Long userId, String roleName) {
        User user = requireUser(userId);
        String fullRoleName = "ROLE_" + roleName.toUpperCase();
        Role role = roleRepository.findByName(fullRoleName)
            .orElseGet(() -> roleRepository.save(new Role(fullRoleName)));
        user.setRoles(new HashSet<>(Set.of(role)));
        return toUserDto(userRepository.save(user));
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private void recalculateBmi(HealthProfile p) {
        if (p.getWeightKg() == null || p.getHeightCm() == null) return;
        BigDecimal heightM = p.getHeightCm().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal bmi = p.getWeightKg()
            .divide(heightM.pow(2, new MathContext(6)), 2, RoundingMode.HALF_UP);
        p.setBmi(bmi);
        p.setBmiCategory(classifyBmi(bmi));
    }

    private String classifyBmi(BigDecimal bmi) {
        if (bmi.compareTo(BMI_UNDERWEIGHT)    < 0) return "Bajo peso";
        if (bmi.compareTo(BMI_NORMAL_MAX)    <= 0) return "Normal";
        if (bmi.compareTo(BMI_OVERWEIGHT_MAX) <= 0) return "Sobrepeso";
        return "Obesidad";
    }

    private int[] parseBloodPressure(String bp) {
        try {
            String cleaned = bp.replace("mmHg", "").strip();
            String[] parts = cleaned.split("/");
            return new int[]{ Integer.parseInt(parts[0].strip()),
                              Integer.parseInt(parts[1].strip()) };
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Formato de presión arterial inválido: '" + bp + "'. Use: '120/80 mmHg'");
        }
    }

    private void validatePhysiologicalBounds(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg != null &&
            (weightKg.compareTo(MIN_WEIGHT_KG) < 0 || weightKg.compareTo(MAX_WEIGHT_KG) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Peso inválido: " + weightKg + " kg. Rango fisiológico: 1–500 kg.");
        }
        if (heightCm != null &&
            (heightCm.compareTo(MIN_HEIGHT_CM) < 0 || heightCm.compareTo(MAX_HEIGHT_CM) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Altura inválida: " + heightCm + " cm. Rango fisiológico: 30–300 cm.");
        }
    }

    private User requireUser(Long id) {
        return userRepository.findByIdWithHealthProfile(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Usuario no encontrado con id: " + id));
    }

    // ── Mappers manuales ──────────────────────────────────────────────────────

    private UserResponseDTO toUserDto(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRoles().stream()
            .map(r -> r.getName()).findFirst().orElse(null));
        dto.setBirthDate(user.getBirthDate());
        dto.setGender(user.getGender());
        dto.setIsEmailVerified(user.getIsEmailVerified());
        dto.setPreferredLanguage(user.getPreferredLanguage());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setHealthProfile(user.getHealthProfile() != null
            ? toHealthProfileDto(user.getHealthProfile())
            : null);
        return dto;
    }

    private HealthProfileResponseDTO toHealthProfileDto(HealthProfile hp) {
        HealthProfileResponseDTO dto = new HealthProfileResponseDTO();
        dto.setUserId(hp.getUser().getId());
        dto.setBloodType(hp.getBloodType());
        dto.setBloodPressure(hp.getBloodPressure());
        dto.setGlucoseLevel(hp.getGlucoseLevel());
        dto.setCholesterolLevel(hp.getCholesterolLevel());
        dto.setAllergies(hp.getAllergies());
        dto.setWeightKg(hp.getWeightKg());
        dto.setHeightCm(hp.getHeightCm());
        dto.setBmi(hp.getBmi());
        dto.setBmiCategory(hp.getBmiCategory());
        dto.setVitalAlertFlag(hp.getVitalAlertFlag());
        dto.setAlertMessage(hp.getAlertMessage());
        return dto;
    }
}