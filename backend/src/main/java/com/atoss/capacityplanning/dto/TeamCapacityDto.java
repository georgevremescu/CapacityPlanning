package com.atoss.capacityplanning.dto;

public record TeamCapacityDto(
    Long teamId,
    String teamName,
    String quarter,
    int workingDaysInQuarter,
    double rawCapacitySp,
    double netCapacitySp,
    double allocatedSp,
    double utilization) {}
