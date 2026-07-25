package com.atoss.capacityplanning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atoss.capacityplanning.dto.InitiativeDetailDto;
import com.atoss.capacityplanning.dto.InitiativeDto;
import com.atoss.capacityplanning.entity.Epic;
import com.atoss.capacityplanning.entity.EpicStatus;
import com.atoss.capacityplanning.entity.Initiative;
import com.atoss.capacityplanning.entity.InitiativeStatus;
import com.atoss.capacityplanning.entity.Team;
import com.atoss.capacityplanning.repository.EpicRepository;
import com.atoss.capacityplanning.repository.InitiativeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitiativeServiceTest {

  @Mock private InitiativeRepository initiativeRepository;
  @Mock private EpicRepository epicRepository;

  private InitiativeService initiativeService;

  @BeforeEach
  void setUp() {
    initiativeService = new InitiativeService(initiativeRepository, epicRepository);
  }

  private static Initiative initiative(long id, String name, InitiativeStatus status) {
    Initiative initiative = new Initiative();
    initiative.setId(id);
    initiative.setName(name);
    initiative.setDescription("desc");
    initiative.setEstimatedStoryPoints(100);
    initiative.setTargetDate(LocalDate.of(2026, 12, 31));
    initiative.setStatus(status);
    initiative.setPriority(1);
    return initiative;
  }

  private static Team team(long id, String name) {
    Team team = new Team(name, 0.2);
    team.setId(id);
    return team;
  }

  private static Epic epic(long id, Team team, Initiative initiative, int storyPoints) {
    Epic epic = new Epic();
    epic.setId(id);
    epic.setName("Epic " + id);
    epic.setTeam(team);
    epic.setInitiative(initiative);
    epic.setStoryPoints(storyPoints);
    epic.setStatus(EpicStatus.COMMITTED);
    return epic;
  }

  @Test
  void findByIdDetailedRollsUpStoryPointsAndDedupesTeams() {
    Initiative initiative = initiative(1L, "Checkout Redesign", InitiativeStatus.COMMITTED);
    Team platform = team(1L, "Platform");
    Team mobile = team(2L, "Mobile");

    when(initiativeRepository.findById(1L)).thenReturn(Optional.of(initiative));
    when(epicRepository.findByInitiativeId(1L))
        .thenReturn(
            List.of(
                epic(1L, platform, initiative, 30),
                epic(2L, mobile, initiative, 25),
                epic(3L, platform, initiative, 20))); // same team as epic 1

    InitiativeDetailDto detail = initiativeService.findByIdDetailed(1L);

    assertThat(detail.rolledUpEpicStoryPoints()).isEqualTo(75);
    assertThat(detail.epics()).hasSize(3);
    assertThat(detail.teamsInvolved()).extracting("id").containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  void findByIdDetailedHandlesInitiativeWithNoEpics() {
    Initiative initiative = initiative(1L, "Empty Initiative", InitiativeStatus.PROPOSED);

    when(initiativeRepository.findById(1L)).thenReturn(Optional.of(initiative));
    when(epicRepository.findByInitiativeId(1L)).thenReturn(List.of());

    InitiativeDetailDto detail = initiativeService.findByIdDetailed(1L);

    assertThat(detail.rolledUpEpicStoryPoints()).isZero();
    assertThat(detail.epics()).isEmpty();
    assertThat(detail.teamsInvolved()).isEmpty();
  }

  @Test
  void findByIdDetailedThrowsWhenInitiativeMissing() {
    when(initiativeRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> initiativeService.findByIdDetailed(1L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void createSavesANewInitiativeWithRequestedFields() {
    when(initiativeRepository.save(any(Initiative.class)))
        .thenAnswer(
            invocation -> {
              Initiative saved = invocation.getArgument(0);
              saved.setId(5L);
              return saved;
            });

    InitiativeDto request =
        new InitiativeDto(
            null,
            "Offline Mode",
            "desc",
            120,
            LocalDate.of(2026, 12, 15),
            InitiativeStatus.PROPOSED,
            2);

    InitiativeDto result = initiativeService.create(request);

    assertThat(result).isEqualTo(new InitiativeDto(5L, "Offline Mode", "desc", 120, LocalDate.of(2026, 12, 15), InitiativeStatus.PROPOSED, 2));
  }

  @Test
  void updateThrowsWhenInitiativeMissing() {
    when(initiativeRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                initiativeService.update(
                    1L,
                    new InitiativeDto(
                        1L, "X", "d", 1, LocalDate.now(), InitiativeStatus.PROPOSED, null)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deleteRemovesAnExistingInitiative() {
    Initiative initiative = initiative(1L, "Checkout Redesign", InitiativeStatus.COMMITTED);
    when(initiativeRepository.findById(1L)).thenReturn(Optional.of(initiative));
    when(epicRepository.findByInitiativeId(1L)).thenReturn(List.of());

    initiativeService.delete(1L);

    verify(initiativeRepository).delete(initiative);
  }

  @Test
  void deleteUnlinksChildEpicsInsteadOfLeavingADanglingForeignKey() {
    Initiative initiative = initiative(1L, "Checkout Redesign", InitiativeStatus.COMMITTED);
    Team platform = team(1L, "Platform");
    Epic epicA = epic(1L, platform, initiative, 30);
    Epic epicB = epic(2L, platform, initiative, 20);

    when(initiativeRepository.findById(1L)).thenReturn(Optional.of(initiative));
    when(epicRepository.findByInitiativeId(1L)).thenReturn(List.of(epicA, epicB));

    initiativeService.delete(1L);

    assertThat(epicA.getInitiative()).isNull();
    assertThat(epicB.getInitiative()).isNull();
    verify(epicRepository).saveAll(anyList());
    verify(initiativeRepository).delete(initiative);
  }
}
