package com.atoss.capacityplanning.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record TeamDto(
    Long id,
    @NotBlank String name,
    @DecimalMin("0.0") @DecimalMax("1.0") double meetingOverheadPercentage,
    @DecimalMin("0.0") @DecimalMax("1.0") double supportLoadOverheadPercentage) {

  public double totalOverheadPercentage() {
    return meetingOverheadPercentage + supportLoadOverheadPercentage;
  }
}
