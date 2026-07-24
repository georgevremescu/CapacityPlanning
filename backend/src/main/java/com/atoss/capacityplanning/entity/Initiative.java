package com.atoss.capacityplanning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

// Teams "involved" in an initiative are derived from the teams of its child epics
// (see EpicRepository/CapacityService) - no direct field, to avoid duplication/drift.
@Entity
@Getter
@Setter
public class Initiative {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  private String description;

  // Top-down estimate at initiative level.
  @Column(nullable = false)
  private double estimatedStoryPoints;

  private LocalDate targetDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InitiativeStatus status;

  // Nullable; supports trade-off ranking.
  // TODO: no formal prioritization algorithm, just a sortable field.
  private Integer priority;

  public Initiative() {}
}
