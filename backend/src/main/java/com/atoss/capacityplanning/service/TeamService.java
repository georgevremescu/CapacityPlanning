package com.atoss.capacityplanning.service;

import com.atoss.capacityplanning.dto.PersonDto;
import com.atoss.capacityplanning.dto.TeamDto;
import com.atoss.capacityplanning.entity.Person;
import com.atoss.capacityplanning.entity.Team;
import com.atoss.capacityplanning.repository.PersonRepository;
import com.atoss.capacityplanning.repository.TeamRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TeamService {

  private final TeamRepository teamRepository;
  private final PersonRepository personRepository;

  public TeamService(TeamRepository teamRepository, PersonRepository personRepository) {
    this.teamRepository = teamRepository;
    this.personRepository = personRepository;
  }

  public List<TeamDto> findAll() {
    return teamRepository.findAll().stream().map(TeamService::toDto).toList();
  }

  public TeamDto findById(Long id) {
    return toDto(getTeamOrThrow(id));
  }

  public TeamDto create(TeamDto request) {
    Team team = new Team(request.name(), request.overheadPercentage());
    return toDto(teamRepository.save(team));
  }

  public TeamDto update(Long id, TeamDto request) {
    Team team = getTeamOrThrow(id);
    team.setName(request.name());
    team.setOverheadPercentage(request.overheadPercentage());
    return toDto(teamRepository.save(team));
  }

  public void delete(Long id) {
    Team team = getTeamOrThrow(id);
    teamRepository.delete(team);
  }

  public List<PersonDto> findPeople(Long teamId) {
    getTeamOrThrow(teamId);
    return personRepository.findByTeamId(teamId).stream().map(TeamService::toDto).toList();
  }

  public PersonDto addPerson(Long teamId, PersonDto request) {
    Team team = getTeamOrThrow(teamId);
    Person person =
        new Person(request.name(), team, request.availabilityFte(), request.velocity());
    return toDto(personRepository.save(person));
  }

  public PersonDto updatePerson(Long teamId, Long personId, PersonDto request) {
    getTeamOrThrow(teamId);
    Person person = getPersonOrThrow(teamId, personId);
    person.setName(request.name());
    person.setAvailabilityFte(request.availabilityFte());
    person.setVelocity(request.velocity());
    return toDto(personRepository.save(person));
  }

  public void deletePerson(Long teamId, Long personId) {
    getTeamOrThrow(teamId);
    Person person = getPersonOrThrow(teamId, personId);
    personRepository.delete(person);
  }

  Team getTeamOrThrow(Long id) {
    return teamRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
  }

  private Person getPersonOrThrow(Long teamId, Long personId) {
    Person person =
        personRepository
            .findById(personId)
            .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + personId));
    if (!person.getTeam().getId().equals(teamId)) {
      throw new ResourceNotFoundException(
          "Person " + personId + " does not belong to team " + teamId);
    }
    return person;
  }

  static TeamDto toDto(Team team) {
    return new TeamDto(team.getId(), team.getName(), team.getOverheadPercentage());
  }

  static PersonDto toDto(Person person) {
    return new PersonDto(
        person.getId(),
        person.getName(),
        person.getTeam().getId(),
        person.getAvailabilityFte(),
        person.getVelocity());
  }
}
