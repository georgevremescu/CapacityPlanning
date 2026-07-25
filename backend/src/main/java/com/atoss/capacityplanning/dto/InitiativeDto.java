package com.atoss.capacityplanning.dto;

import com.atoss.capacityplanning.entity.InitiativeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record InitiativeDto(
    Long id,
    @NotBlank String name,
    String description,
    @PositiveOrZero int estimatedStoryPoints,
    LocalDate targetDate,
    @NotNull InitiativeStatus status,
    Integer priority) {}
