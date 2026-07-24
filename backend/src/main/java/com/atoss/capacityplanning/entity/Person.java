package com.atoss.capacityplanning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

// A person belongs to exactly one team.
// TODO: cross-team allocation not supported.
@Entity
@Getter
@Setter
public class Person {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  // 0.0-1.0, e.g. 0.8 = 80%.
  // TODO: constant over time; time-varying availability (leave, ramp-up/down) not modeled.
  @Column(nullable = false)
  private double availabilityFte;

  // Story points per working day.
  // TODO: assumes flat personal velocity; no per-skill or per-team-context variation.
  @Column(nullable = false)
  private double velocity;

  public Person() {}

  public Person(String name, Team team, double availabilityFte, double velocity) {
    this.name = name;
    this.team = team;
    this.availabilityFte = availabilityFte;
    this.velocity = velocity;
  }
}
