package com.atoss.capacityplanning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.atoss.capacityplanning.dto.CapacityOverviewDto;
import com.atoss.capacityplanning.dto.TeamCapacityDto;
import com.atoss.capacityplanning.entity.Epic;
import com.atoss.capacityplanning.entity.EpicStatus;
import com.atoss.capacityplanning.entity.Person;
import com.atoss.capacityplanning.entity.Team;
import com.atoss.capacityplanning.repository.EpicRepository;
import com.atoss.capacityplanning.repository.PersonRepository;
import com.atoss.capacityplanning.repository.TeamRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CapacityServiceTest {

  @Mock private TeamRepository teamRepository;
  @Mock private PersonRepository personRepository;
  @Mock private EpicRepository epicRepository;

  private CapacityService capacityService;

  private Team team;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    capacityService = new CapacityService(teamRepository, personRepository, epicRepository);
    team = new Team("Platform", 0.12, 0.08);
    team.setId(1L);
  }

  private static Person person(Team team, double availabilityFte, double velocity) {
    return new Person("Person", team, availabilityFte, velocity);
  }

  private static Epic epic(Team team, int storyPoints, LocalDate dueDate, EpicStatus status) {
    Epic epic = new Epic();
    epic.setName("Epic");
    epic.setTeam(team);
    epic.setStoryPoints(storyPoints);
    epic.setDueDate(dueDate);
    epic.setStatus(status);
    return epic;
  }

  @Test
  void computesRawAndNetCapacityFromRoster() {
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(personRepository.findByTeamId(1L))
        .thenReturn(
            List.of(
                person(team, 1.0, 6.0), // 1.0 * 6.0 * 62 = 372
                person(team, 0.8, 5.0))); // 0.8 * 5.0 * 62 = 248
    when(epicRepository.findByTeamId(1L)).thenReturn(List.of());

    TeamCapacityDto result = capacityService.forTeam(1L, "2026-Q3", CapacityMode.COMMITTED);

    assertThat(result.rawCapacitySp()).isCloseTo(620.0, within(0.001));
    // net = raw * (1 - 0.20)
    assertThat(result.netCapacitySp()).isCloseTo(496.0, within(0.001));
    assertThat(result.workingDaysInQuarter()).isEqualTo(62);
  }

  @Test
  void committedModeCountsOnlyCommittedInProgressAndDoneEpicsInQuarter() {
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(personRepository.findByTeamId(1L)).thenReturn(List.of(person(team, 1.0, 10.0)));
    when(epicRepository.findByTeamId(1L))
        .thenReturn(
            List.of(
                epic(team, 10, LocalDate.of(2026, 8, 1), EpicStatus.COMMITTED),
                epic(team, 20, LocalDate.of(2026, 8, 1), EpicStatus.IN_PROGRESS),
                epic(team, 30, LocalDate.of(2026, 8, 1), EpicStatus.DONE),
                epic(team, 40, LocalDate.of(2026, 8, 1), EpicStatus.PROPOSED),
                epic(team, 50, LocalDate.of(2026, 8, 1), EpicStatus.CANCELLED)));

    TeamCapacityDto result = capacityService.forTeam(1L, "2026-Q3", CapacityMode.COMMITTED);

    assertThat(result.allocatedSp()).isEqualTo(60.0); // 10 + 20 + 30
  }

  @Test
  void simulatedModeAlsoIncludesProposedButStillExcludesCancelled() {
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(personRepository.findByTeamId(1L)).thenReturn(List.of(person(team, 1.0, 10.0)));
    when(epicRepository.findByTeamId(1L))
        .thenReturn(
            List.of(
                epic(team, 10, LocalDate.of(2026, 8, 1), EpicStatus.COMMITTED),
                epic(team, 40, LocalDate.of(2026, 8, 1), EpicStatus.PROPOSED),
                epic(team, 50, LocalDate.of(2026, 8, 1), EpicStatus.CANCELLED)));

    TeamCapacityDto result = capacityService.forTeam(1L, "2026-Q3", CapacityMode.SIMULATED);

    assertThat(result.allocatedSp()).isEqualTo(50.0); // 10 + 40
  }

  @Test
  void epicsOutsideTheRequestedQuarterAreExcluded() {
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(personRepository.findByTeamId(1L)).thenReturn(List.of(person(team, 1.0, 10.0)));
    when(epicRepository.findByTeamId(1L))
        .thenReturn(
            List.of(
                epic(team, 10, LocalDate.of(2026, 8, 1), EpicStatus.COMMITTED), // in Q3
                epic(team, 99, LocalDate.of(2026, 11, 1), EpicStatus.COMMITTED))); // in Q4

    TeamCapacityDto result = capacityService.forTeam(1L, "2026-Q3", CapacityMode.COMMITTED);

    assertThat(result.allocatedSp()).isEqualTo(10.0);
  }

  @Test
  void utilizationIsZeroRatherThanDivideByZeroWhenTeamHasNoCapacity() {
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(personRepository.findByTeamId(1L)).thenReturn(List.of());
    when(epicRepository.findByTeamId(1L)).thenReturn(List.of());

    TeamCapacityDto result = capacityService.forTeam(1L, "2026-Q3", CapacityMode.COMMITTED);

    assertThat(result.netCapacitySp()).isZero();
    assertThat(result.utilization()).isZero();
  }

  @Test
  void forTeamThrowsWhenTeamDoesNotExist() {
    when(teamRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> capacityService.forTeam(99L, "2026-Q3", CapacityMode.COMMITTED))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void overviewAggregatesNetAndAllocatedCapacityAcrossTeams() {
    Team mobile = new Team("Mobile", 0.0, 0.0);
    mobile.setId(2L);

    when(teamRepository.findAll()).thenReturn(List.of(team, mobile));

    when(personRepository.findByTeamId(1L)).thenReturn(List.of(person(team, 1.0, 10.0)));
    when(epicRepository.findByTeamId(1L))
        .thenReturn(List.of(epic(team, 100, LocalDate.of(2026, 8, 1), EpicStatus.COMMITTED)));

    when(personRepository.findByTeamId(2L)).thenReturn(List.of(person(mobile, 1.0, 10.0)));
    when(epicRepository.findByTeamId(2L))
        .thenReturn(List.of(epic(mobile, 50, LocalDate.of(2026, 8, 1), EpicStatus.COMMITTED)));

    CapacityOverviewDto overview = capacityService.overview("2026-Q3", CapacityMode.COMMITTED);

    assertThat(overview.teams()).hasSize(2);
    assertThat(overview.orgAllocatedSp()).isEqualTo(150.0);
    // team net = 620 * 0.8 = 496, mobile net = 620 * 1.0 = 620 -> org net = 1116
    assertThat(overview.orgNetCapacitySp()).isCloseTo(1116.0, within(0.001));
    assertThat(overview.orgUtilization())
        .isCloseTo(150.0 / 1116.0, within(0.0001));
  }

  @Test
  void overviewUtilizationIsZeroWhenThereAreNoTeams() {
    when(teamRepository.findAll()).thenReturn(List.of());

    CapacityOverviewDto overview = capacityService.overview("2026-Q3", CapacityMode.COMMITTED);

    assertThat(overview.teams()).isEmpty();
    assertThat(overview.orgUtilization()).isZero();
  }
}
