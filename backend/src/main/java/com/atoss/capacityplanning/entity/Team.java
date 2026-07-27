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

  // e.g. 0.1 = 10% of capacity lost to recurring meetings/admin.
  @Column(nullable = false)
  private double meetingOverheadPercentage;

  // e.g. 0.1 = 10% of capacity lost to ad-hoc support/interrupt work.
  @Column(nullable = false)
  private double supportLoadOverheadPercentage;

  public Team() {}

  public Team(String name, double meetingOverheadPercentage, double supportLoadOverheadPercentage) {
    this.name = name;
    this.meetingOverheadPercentage = meetingOverheadPercentage;
    this.supportLoadOverheadPercentage = supportLoadOverheadPercentage;
  }

  public double getTotalOverheadPercentage() {
    return meetingOverheadPercentage + supportLoadOverheadPercentage;
  }
}
