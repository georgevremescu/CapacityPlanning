package com.atoss.capacityplanning.controller;

import com.atoss.capacityplanning.dto.PersonDto;
import com.atoss.capacityplanning.dto.TeamDto;
import com.atoss.capacityplanning.service.TeamService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

  private final TeamService teamService;

  public TeamController(TeamService teamService) {
    this.teamService = teamService;
  }

  @GetMapping
  public List<TeamDto> findAll() {
    return teamService.findAll();
  }

  @GetMapping("/{id}")
  public TeamDto findById(@PathVariable Long id) {
    return teamService.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TeamDto create(@RequestBody TeamDto request) {
    return teamService.create(request);
  }

  @PutMapping("/{id}")
  public TeamDto update(@PathVariable Long id, @RequestBody TeamDto request) {
    return teamService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    teamService.delete(id);
  }

  // People are only ever accessed nested under their team.
  @GetMapping("/{teamId}/people")
  public List<PersonDto> findPeople(@PathVariable Long teamId) {
    return teamService.findPeople(teamId);
  }

  @PostMapping("/{teamId}/people")
  @ResponseStatus(HttpStatus.CREATED)
  public PersonDto addPerson(@PathVariable Long teamId, @RequestBody PersonDto request) {
    return teamService.addPerson(teamId, request);
  }

  @PutMapping("/{teamId}/people/{personId}")
  public PersonDto updatePerson(
      @PathVariable Long teamId, @PathVariable Long personId, @RequestBody PersonDto request) {
    return teamService.updatePerson(teamId, personId, request);
  }

  @DeleteMapping("/{teamId}/people/{personId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePerson(@PathVariable Long teamId, @PathVariable Long personId) {
    teamService.deletePerson(teamId, personId);
  }
}
