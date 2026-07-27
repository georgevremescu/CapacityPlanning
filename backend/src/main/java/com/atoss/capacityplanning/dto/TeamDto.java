package com.atoss.capacityplanning.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamDto(Long id, @NotBlank String name) {}
