package com.spire.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    private String description;
    private String videoUrl;
    private Integer orderIndex;
    private Integer durationMinutes;
    private Boolean isFree;
}
