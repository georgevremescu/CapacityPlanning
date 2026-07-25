package com.atoss.capacityplanning.dto;

import com.atoss.capacityplanning.entity.EpicStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record EpicDto(
    Long id,
    @NotBlank String name,
    Long initiativeId,
    String initiativeName,
    @NotNull Long teamId,
    String teamName,
    @PositiveOrZero int storyPoints,
    LocalDate dueDate,
    @NotNull EpicStatus status) {}
