package com.atoss.capacityplanning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Team {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  // e.g. 0.2 = 20% of capacity lost to meetings/support/admin.
  // TODO: single blended number; split into meeting overhead vs support-load overhead later.
  @Column(nullable = false)
  private double overheadPercentage;

  public Team() {}

  public Team(String name, double overheadPercentage) {
    this.name = name;
    this.overheadPercentage = overheadPercentage;
  }
}
