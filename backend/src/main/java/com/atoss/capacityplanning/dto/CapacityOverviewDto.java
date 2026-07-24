package com.atoss.capacityplanning.dto;

import java.util.List;

public record CapacityOverviewDto(
    String quarter,
    List<TeamCapacityDto> teams,
    double orgNetCapacitySp,
    double orgAllocatedSp,
    double orgUtilization) {}
