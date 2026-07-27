package com.atoss.capacityplanning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atoss.capacityplanning.dto.PersonDto;
import com.atoss.capacityplanning.dto.TeamDto;
import com.atoss.capacityplanning.entity.Person;
import com.atoss.capacityplanning.entity.Team;
import com.atoss.capacityplanning.repository.PersonRepository;
import com.atoss.capacityplanning.repository.TeamRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

  @Mock private TeamRepository teamRepository;
  @Mock private PersonRepository personRepository;

  private TeamService teamService;

  @BeforeEach
  void setUp() {
    teamService = new TeamService(teamRepository, personRepository);
  }

  private static Team team(long id, String name) {
    Team team = new Team(name);
    team.setId(id);
    return team;
  }

  private static Person personOf(long id, Team team, String name) {
    Person person = new Person(name, team, 1.0, 5.0, 0.1, 0.05);
    person.setId(id);
    return person;
  }

  @Test
  void findAllMapsEntitiesToDtos() {
    when(teamRepository.findAll()).thenReturn(List.of(team(1L, "Platform")));

    List<TeamDto> result = teamService.findAll();

    assertThat(result).containsExactly(new TeamDto(1L, "Platform"));
  }

  @Test
  void findByIdThrowsWhenMissing() {
    when(teamRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamService.findById(1L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void createSavesANewTeam() {
    when(teamRepository.save(any(Team.class)))
        .thenAnswer(
            invocation -> {
              Team saved = invocation.getArgument(0);
              saved.setId(42L);
              return saved;
            });

    TeamDto result = teamService.create(new TeamDto(null, "Data"));

    assertThat(result).isEqualTo(new TeamDto(42L, "Data"));
  }

  @Test
  void updateOverwritesExistingTeamFields() {
    Team existing = team(1L, "Old Name");
    when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TeamDto result = teamService.update(1L, new TeamDto(1L, "New Name"));

    assertThat(result).isEqualTo(new TeamDto(1L, "New Name"));
  }

  @Test
  void deleteRemovesAnExistingTeam() {
    Team existing = team(1L, "Platform");
    when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));

    teamService.delete(1L);

    verify(teamRepository).delete(existing);
  }

  @Test
  void deleteThrowsWhenTeamMissing() {
    when(teamRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamService.delete(1L)).isInstanceOf(ResourceNotFoundException.class);
    verify(teamRepository, never()).delete(any());
  }

  @Test
  void findPeopleThrowsWhenTeamMissing() {
    when(teamRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamService.findPeople(1L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void findPeopleReturnsRosterForExistingTeam() {
    Team platform = team(1L, "Platform");
    when(teamRepository.findById(1L)).thenReturn(Optional.of(platform));
    when(personRepository.findByTeamId(1L))
        .thenReturn(List.of(personOf(10L, platform, "Alice")));

    List<PersonDto> result = teamService.findPeople(1L);

    assertThat(result).containsExactly(new PersonDto(10L, "Alice", 1L, 1.0, 5.0, 0.1, 0.05));
  }

  @Test
  void addPersonAttachesTheNewPersonToTheTeam() {
    Team platform = team(1L, "Platform");
    when(teamRepository.findById(1L)).thenReturn(Optional.of(platform));
    when(personRepository.save(any(Person.class)))
        .thenAnswer(
            invocation -> {
              Person saved = invocation.getArgument(0);
              saved.setId(99L);
              return saved;
            });

    PersonDto result =
        teamService.addPerson(1L, new PersonDto(null, "Bob", null, 0.8, 4.0, 0.1, 0.1));

    assertThat(result).isEqualTo(new PersonDto(99L, "Bob", 1L, 0.8, 4.0, 0.1, 0.1));
  }

  @Test
  void addPersonRejectsOverheadsThatSumAboveOne() {
    Team platform = team(1L, "Platform");
    when(teamRepository.findById(1L)).thenReturn(Optional.of(platform));

    assertThatThrownBy(
            () -> teamService.addPerson(1L, new PersonDto(null, "Bob", null, 0.8, 4.0, 0.6, 0.5)))
        .isInstanceOf(IllegalArgumentException.class);
    verify(personRepository, never()).save(any());
  }

  @Test
  void updatePersonThrowsWhenPersonBelongsToAnotherTeam() {
    Team platform = team(1L, "Platform");
    Team mobile = team(2L, "Mobile");
    Person personOnMobile = personOf(10L, mobile, "Carol");

    when(teamRepository.findById(1L)).thenReturn(Optional.of(platform));
    when(personRepository.findById(10L)).thenReturn(Optional.of(personOnMobile));

    assertThatThrownBy(
            () ->
                teamService.updatePerson(
                    1L, 10L, new PersonDto(10L, "Carol", 1L, 1.0, 5.0, 0.1, 0.05)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void updatePersonRejectsOverheadsThatSumAboveOne() {
    Team platform = team(1L, "Platform");
    when(teamRepository.findById(1L)).thenReturn(Optional.of(platform));

    assertThatThrownBy(
            () ->
                teamService.updatePerson(
                    1L, 10L, new PersonDto(10L, "Alice", 1L, 1.0, 5.0, 0.7, 0.4)))
        .isInstanceOf(IllegalArgumentException.class);
    verify(personRepository, never()).save(any());
  }

  @Test
  void deletePersonRemovesAMatchingPerson() {
    Team platform = team(1L, "Platform");
    Person person = personOf(10L, platform, "Alice");

    when(teamRepository.findById(1L)).thenReturn(Optional.of(platform));
    when(personRepository.findById(10L)).thenReturn(Optional.of(person));

    teamService.deletePerson(1L, 10L);

    verify(personRepository, times(1)).delete(person);
  }
}
