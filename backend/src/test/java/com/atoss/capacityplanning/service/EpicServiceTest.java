package com.atoss.capacityplanning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atoss.capacityplanning.dto.EpicDto;
import com.atoss.capacityplanning.entity.Epic;
import com.atoss.capacityplanning.entity.EpicStatus;
import com.atoss.capacityplanning.entity.Initiative;
import com.atoss.capacityplanning.entity.InitiativeStatus;
import com.atoss.capacityplanning.entity.Team;
import com.atoss.capacityplanning.repository.EpicRepository;
import com.atoss.capacityplanning.repository.InitiativeRepository;
import com.atoss.capacityplanning.repository.TeamRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EpicServiceTest {

  @Mock private EpicRepository epicRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private InitiativeRepository initiativeRepository;

  private EpicService epicService;

  @BeforeEach
  void setUp() {
    epicService = new EpicService(epicRepository, teamRepository, initiativeRepository);
  }

  private static Team team(long id, String name) {
    Team team = new Team(name, 0.2, 0.0);
    team.setId(id);
    return team;
  }

  private static Initiative initiative(long id, String name) {
    Initiative initiative = new Initiative();
    initiative.setId(id);
    initiative.setName(name);
    initiative.setStatus(InitiativeStatus.COMMITTED);
    return initiative;
  }

  @Test
  void createResolvesTeamAndInitiativeThenSaves() {
    Team platform = team(1L, "Platform");
    Initiative checkoutRedesign = initiative(2L, "Checkout Redesign");

    when(teamRepository.findById(1L)).thenReturn(Optional.of(platform));
    when(initiativeRepository.findById(2L)).thenReturn(Optional.of(checkoutRedesign));
    when(epicRepository.save(any(Epic.class)))
        .thenAnswer(
            invocation -> {
              Epic saved = invocation.getArgument(0);
              saved.setId(10L);
              return saved;
            });

    EpicDto request =
        new EpicDto(
            null, "Payment API revamp", 2L, null, 1L, null, 30, LocalDate.of(2026, 8, 15), EpicStatus.COMMITTED);

    EpicDto result = epicService.create(request);

    assertThat(result)
        .isEqualTo(
            new EpicDto(
                10L,
                "Payment API revamp",
                2L,
                "Checkout Redesign",
                1L,
                "Platform",
                30,
                LocalDate.of(2026, 8, 15),
                EpicStatus.COMMITTED));
  }

  @Test
  void createAllowsAStandaloneEpicWithNoInitiative() {
    Team platform = team(1L, "Platform");
    when(teamRepository.findById(1L)).thenReturn(Optional.of(platform));
    when(epicRepository.save(any(Epic.class)))
        .thenAnswer(
            invocation -> {
              Epic saved = invocation.getArgument(0);
              saved.setId(11L);
              return saved;
            });

    EpicDto request =
        new EpicDto(
            null, "Tech debt cleanup", null, null, 1L, null, 15, LocalDate.of(2026, 8, 30), EpicStatus.COMMITTED);

    EpicDto result = epicService.create(request);

    assertThat(result.initiativeId()).isNull();
    assertThat(result.initiativeName()).isNull();
    assertThat(result.teamName()).isEqualTo("Platform");
  }

  @Test
  void createThrowsWhenTeamDoesNotExist() {
    when(teamRepository.findById(99L)).thenReturn(Optional.empty());

    EpicDto request =
        new EpicDto(null, "Epic", null, null, 99L, null, 10, LocalDate.now(), EpicStatus.PROPOSED);

    assertThatThrownBy(() -> epicService.create(request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void createThrowsWhenInitiativeDoesNotExist() {
    Team platform = team(1L, "Platform");
    when(teamRepository.findById(1L)).thenReturn(Optional.of(platform));
    when(initiativeRepository.findById(99L)).thenReturn(Optional.empty());

    EpicDto request =
        new EpicDto(null, "Epic", 99L, null, 1L, null, 10, LocalDate.now(), EpicStatus.PROPOSED);

    assertThatThrownBy(() -> epicService.create(request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deleteRemovesAnExistingEpic() {
    Epic epic = new Epic();
    epic.setId(1L);
    epic.setTeam(team(1L, "Platform"));
    when(epicRepository.findById(1L)).thenReturn(Optional.of(epic));

    epicService.delete(1L);

    verify(epicRepository).delete(epic);
  }

  @Test
  void findByIdThrowsWhenEpicMissing() {
    when(epicRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> epicService.findById(1L)).isInstanceOf(ResourceNotFoundException.class);
  }
}
