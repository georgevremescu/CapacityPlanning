package com.atoss.capacityplanning.service;

import com.atoss.capacityplanning.dto.CapacityOverviewDto;
import com.atoss.capacityplanning.dto.TeamCapacityDto;
import com.atoss.capacityplanning.entity.Epic;
import com.atoss.capacityplanning.entity.EpicStatus;
import com.atoss.capacityplanning.entity.Person;
import com.atoss.capacityplanning.entity.Team;
import com.atoss.capacityplanning.repository.EpicRepository;
import com.atoss.capacityplanning.repository.PersonRepository;
import com.atoss.capacityplanning.repository.TeamRepository;
import com.atoss.capacityplanning.util.Quarter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CapacityService {

  // Epics in these statuses always count as demand, in both modes.
  private static final Set<EpicStatus> COMMITTED_STATUSES =
      EnumSet.of(EpicStatus.COMMITTED, EpicStatus.IN_PROGRESS, EpicStatus.DONE);

  // TODO: initiative.status is not cross-checked against its epics' statuses here;
  // the capacity calculation is driven purely by epic-level status.
  private final TeamRepository teamRepository;
  private final PersonRepository personRepository;
  private final EpicRepository epicRepository;

  public CapacityService(
      TeamRepository teamRepository, PersonRepository personRepository, EpicRepository epicRepository) {
    this.teamRepository = teamRepository;
    this.personRepository = personRepository;
    this.epicRepository = epicRepository;
  }

  public TeamCapacityDto forTeam(Long teamId, String quarterValue, CapacityMode mode) {
    Team team =
        teamRepository
            .findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));
    Quarter quarter = Quarter.parse(quarterValue);
    return calculate(team, quarter, mode);
  }

  public CapacityOverviewDto overview(String quarterValue, CapacityMode mode) {
    Quarter quarter = Quarter.parse(quarterValue);
    List<TeamCapacityDto> teamCapacities =
        teamRepository.findAll().stream().map(team -> calculate(team, quarter, mode)).toList();

    double orgNetCapacitySp = teamCapacities.stream().mapToDouble(TeamCapacityDto::netCapacitySp).sum();
    double orgAllocatedSp = teamCapacities.stream().mapToDouble(TeamCapacityDto::allocatedSp).sum();
    double orgUtilization = orgNetCapacitySp == 0 ? 0 : orgAllocatedSp / orgNetCapacitySp;

    return new CapacityOverviewDto(
        quarter.toString(), teamCapacities, orgNetCapacitySp, orgAllocatedSp, orgUtilization);
  }

  private TeamCapacityDto calculate(Team team, Quarter quarter, CapacityMode mode) {
    List<Person> people = personRepository.findByTeamId(team.getId());
    int workingDays = Quarter.WORKING_DAYS_IN_QUARTER;

    double rawCapacitySp =
        people.stream()
            .mapToDouble(p -> p.getAvailabilityFte() * p.getVelocity() * workingDays)
            .sum();
    double netCapacitySp = rawCapacitySp * (1 - team.getOverheadPercentage());

    Set<EpicStatus> includedStatuses = EnumSet.copyOf(COMMITTED_STATUSES);
    if (mode == CapacityMode.SIMULATED) {
      includedStatuses.add(EpicStatus.PROPOSED);
    }

    double allocatedSp =
        epicRepository.findByTeamId(team.getId()).stream()
            .filter(epic -> includedStatuses.contains(epic.getStatus()))
            .filter(epic -> quarter.contains(epic.getDueDate()))
            .mapToDouble(Epic::getStoryPoints)
            .sum();

    double utilization = netCapacitySp == 0 ? 0 : allocatedSp / netCapacitySp;

    return new TeamCapacityDto(
        team.getId(),
        team.getName(),
        quarter.toString(),
        workingDays,
        rawCapacitySp,
        netCapacitySp,
        allocatedSp,
        utilization);
  }
}
