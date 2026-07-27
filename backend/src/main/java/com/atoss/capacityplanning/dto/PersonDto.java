package com.atoss.capacityplanning.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record PersonDto(
    Long id,
    @NotBlank String name,
    Long teamId,
    @DecimalMin("0.0") @DecimalMax("1.0") double availabilityFte,
    @PositiveOrZero double velocity,
    @DecimalMin("0.0") @DecimalMax("1.0") double meetingOverheadPercentage,
    @DecimalMin("0.0") @DecimalMax("1.0") double supportLoadOverheadPercentage) {

  public double totalOverheadPercentage() {
    return meetingOverheadPercentage + supportLoadOverheadPercentage;
  }
}
