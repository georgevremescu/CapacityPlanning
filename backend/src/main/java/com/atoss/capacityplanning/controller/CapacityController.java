package com.atoss.capacityplanning.controller;

import com.atoss.capacityplanning.dto.CapacityOverviewDto;
import com.atoss.capacityplanning.dto.TeamCapacityDto;
import com.atoss.capacityplanning.service.CapacityMode;
import com.atoss.capacityplanning.service.CapacityService;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/capacity")
public class CapacityController {

  private final CapacityService capacityService;

  public CapacityController(CapacityService capacityService) {
    this.capacityService = capacityService;
  }

  @GetMapping("/teams/{teamId}")
  public TeamCapacityDto forTeam(
      @PathVariable Long teamId,
      @RequestParam String quarter,
      @RequestParam(defaultValue = "committed") String mode) {
    return capacityService.forTeam(teamId, quarter, parseMode(mode));
  }

  @GetMapping("/overview")
  public CapacityOverviewDto overview(
      @RequestParam String quarter, @RequestParam(defaultValue = "committed") String mode) {
    return capacityService.overview(quarter, parseMode(mode));
  }

  private CapacityMode parseMode(String mode) {
    return switch (mode.toLowerCase(Locale.ROOT)) {
      case "committed" -> CapacityMode.COMMITTED;
      case "simulated" -> CapacityMode.SIMULATED;
      default ->
          throw new IllegalArgumentException(
              "Invalid mode '" + mode + "', expected 'committed' or 'simulated'");
    };
  }
}
