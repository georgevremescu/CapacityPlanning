package com.atoss.capacityplanning.dto;

public record PersonDto(
    Long id, String name, Long teamId, double availabilityFte, double velocity) {}
