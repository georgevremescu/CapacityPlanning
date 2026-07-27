package com.atoss.capacityplanning.service;

public enum CapacityMode {
  COMMITTED,
  SIMULATED;

  // Lowercase query-param spellings accepted by CapacityController; kept here so the
  // controller's default value, switch, and error message can't drift from each other.
  public static final String COMMITTED_PARAM = "committed";
  public static final String SIMULATED_PARAM = "simulated";
}
