package com.aurahealth.api.auradtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

/** DTO compartido — usado por Stefany (HU08) y Omar (HU09). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationResponseDTO {

    private Long      id;
    private Long      userId;
    private String    name;
    private String    dosage;
    private String    frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean   isSharedWithPartner;
    private Boolean   isCompletedToday;
}
