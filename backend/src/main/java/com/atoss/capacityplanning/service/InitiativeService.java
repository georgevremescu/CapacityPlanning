package com.atoss.capacityplanning.service;

import com.atoss.capacityplanning.dto.EpicDto;
import com.atoss.capacityplanning.dto.InitiativeDetailDto;
import com.atoss.capacityplanning.dto.InitiativeDto;
import com.atoss.capacityplanning.dto.TeamDto;
import com.atoss.capacityplanning.entity.Epic;
import com.atoss.capacityplanning.entity.Initiative;
import com.atoss.capacityplanning.repository.EpicRepository;
import com.atoss.capacityplanning.repository.InitiativeRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InitiativeService {

  private final InitiativeRepository initiativeRepository;
  private final EpicRepository epicRepository;

  public InitiativeService(
      InitiativeRepository initiativeRepository, EpicRepository epicRepository) {
    this.initiativeRepository = initiativeRepository;
    this.epicRepository = epicRepository;
  }

  public List<InitiativeDto> findAll() {
    return initiativeRepository.findAll().stream().map(InitiativeService::toDto).toList();
  }

  public InitiativeDetailDto findByIdDetailed(Long id) {
    Initiative initiative = getInitiativeOrThrow(id);
    List<Epic> epics = epicRepository.findByInitiativeId(id);

    List<EpicDto> epicDtos = epics.stream().map(EpicService::toDto).toList();
    int rolledUp = epics.stream().mapToInt(Epic::getStoryPoints).sum();

    // Teams involved are derived from the teams of the child epics, de-duplicated.
    Map<Long, TeamDto> teamsById = new LinkedHashMap<>();
    for (Epic epic : epics) {
      teamsById.putIfAbsent(
          epic.getTeam().getId(), new TeamDto(epic.getTeam().getId(), epic.getTeam().getName()));
    }
    List<TeamDto> teamsInvolved =
        teamsById.values().stream().sorted(Comparator.comparing(TeamDto::name)).toList();

    return new InitiativeDetailDto(toDto(initiative), epicDtos, rolledUp, teamsInvolved);
  }

  public InitiativeDto create(InitiativeDto request) {
    Initiative initiative = new Initiative();
    applyRequest(initiative, request);
    return toDto(initiativeRepository.save(initiative));
  }

  public InitiativeDto update(Long id, InitiativeDto request) {
    Initiative initiative = getInitiativeOrThrow(id);
    applyRequest(initiative, request);
    return toDto(initiativeRepository.save(initiative));
  }

  public void delete(Long id) {
    Initiative initiative = getInitiativeOrThrow(id);
    // Epics aren't deleted with their initiative - they become standalone, matching
    // the "unlinked" behavior the UI promises when confirming this action.
    List<Epic> epics = epicRepository.findByInitiativeId(id);
    epics.forEach(epic -> epic.setInitiative(null));
    epicRepository.saveAll(epics);
    initiativeRepository.delete(initiative);
  }

  private void applyRequest(Initiative initiative, InitiativeDto request) {
    initiative.setName(request.name());
    initiative.setDescription(request.description());
    initiative.setEstimatedStoryPoints(request.estimatedStoryPoints());
    initiative.setTargetDate(request.targetDate());
    initiative.setStatus(request.status());
    initiative.setPriority(request.priority());
  }

  Initiative getInitiativeOrThrow(Long id) {
    return initiativeRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Initiative not found: " + id));
  }

  static InitiativeDto toDto(Initiative initiative) {
    return new InitiativeDto(
        initiative.getId(),
        initiative.getName(),
        initiative.getDescription(),
        initiative.getEstimatedStoryPoints(),
        initiative.getTargetDate(),
        initiative.getStatus(),
        initiative.getPriority());
  }
}
