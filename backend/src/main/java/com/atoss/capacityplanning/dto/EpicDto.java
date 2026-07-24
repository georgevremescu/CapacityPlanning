package com.atoss.capacityplanning.dto;

import com.atoss.capacityplanning.entity.EpicStatus;
import java.time.LocalDate;

public record EpicDto(
    Long id,
    String name,
    Long initiativeId,
    String initiativeName,
    Long teamId,
    String teamName,
    double storyPoints,
    LocalDate dueDate,
    EpicStatus status) {}
