package com.aurahealth.api.auradtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

/** DTO compartido — usado por Stefany (HU12) y Omar (HU11). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResponseDTO {

    private Long      id;
    private Long      userId;
    private String    title;
    private String    reminderType;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private Boolean   isDone;
}
