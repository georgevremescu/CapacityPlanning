package com.atoss.capacityplanning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Epic {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  // Nullable so standalone work not tied to an initiative can still be planned.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "initiative_id")
  private Initiative initiative;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @Column(nullable = false)
  private double storyPoints;

  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EpicStatus status;

  public Epic() {}
}
