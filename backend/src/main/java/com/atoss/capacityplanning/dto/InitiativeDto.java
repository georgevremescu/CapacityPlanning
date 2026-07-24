package com.atoss.capacityplanning.dto;

import com.atoss.capacityplanning.entity.InitiativeStatus;
import java.time.LocalDate;

public record InitiativeDto(
    Long id,
    String name,
    String description,
    double estimatedStoryPoints,
    LocalDate targetDate,
    InitiativeStatus status,
    Integer priority) {}
