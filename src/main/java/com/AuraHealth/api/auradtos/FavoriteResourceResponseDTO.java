package com.aurahealth.api.auradtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResourceResponseDTO {
    private Long resourceId;
    private String title;
    private String category;
    private String imageUrl;
    private String formatType;
    private LocalDateTime savedAt;
}