package com.atoss.capacityplanning.dto;

import java.util.List;

// Detail view: child epics, rolled-up story points, and derived teams involved
// (derived from the teams of the child epics - not a stored field).
public record InitiativeDetailDto(
    InitiativeDto initiative,
    List<EpicDto> epics,
    double rolledUpEpicStoryPoints,
    List<TeamDto> teamsInvolved) {}
