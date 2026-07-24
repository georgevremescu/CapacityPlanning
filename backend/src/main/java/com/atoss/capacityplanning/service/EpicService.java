package com.atoss.capacityplanning.service;

import com.atoss.capacityplanning.dto.EpicDto;
import com.atoss.capacityplanning.entity.Epic;
import com.atoss.capacityplanning.entity.Initiative;
import com.atoss.capacityplanning.entity.Team;
import com.atoss.capacityplanning.repository.EpicRepository;
import com.atoss.capacityplanning.repository.InitiativeRepository;
import com.atoss.capacityplanning.repository.TeamRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EpicService {

  private final EpicRepository epicRepository;
  private final TeamRepository teamRepository;
  private final InitiativeRepository initiativeRepository;

  public EpicService(
      EpicRepository epicRepository,
      TeamRepository teamRepository,
      InitiativeRepository initiativeRepository) {
    this.epicRepository = epicRepository;
    this.teamRepository = teamRepository;
    this.initiativeRepository = initiativeRepository;
  }

  public List<EpicDto> findAll() {
    return epicRepository.findAll().stream().map(EpicService::toDto).toList();
  }

  public EpicDto findById(Long id) {
    return toDto(getEpicOrThrow(id));
  }

  public EpicDto create(EpicDto request) {
    Epic epic = new Epic();
    applyRequest(epic, request);
    return toDto(epicRepository.save(epic));
  }

  public EpicDto update(Long id, EpicDto request) {
    Epic epic = getEpicOrThrow(id);
    applyRequest(epic, request);
    return toDto(epicRepository.save(epic));
  }

  public void delete(Long id) {
    epicRepository.delete(getEpicOrThrow(id));
  }

  private void applyRequest(Epic epic, EpicDto request) {
    Team team =
        teamRepository
            .findById(request.teamId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Team not found: " + request.teamId()));
    Initiative initiative = null;
    if (request.initiativeId() != null) {
      initiative =
          initiativeRepository
              .findById(request.initiativeId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Initiative not found: " + request.initiativeId()));
    }
    epic.setName(request.name());
    epic.setTeam(team);
    epic.setInitiative(initiative);
    epic.setStoryPoints(request.storyPoints());
    epic.setDueDate(request.dueDate());
    epic.setStatus(request.status());
  }

  Epic getEpicOrThrow(Long id) {
    return epicRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Epic not found: " + id));
  }

  static EpicDto toDto(Epic epic) {
    Initiative initiative = epic.getInitiative();
    return new EpicDto(
        epic.getId(),
        epic.getName(),
        initiative != null ? initiative.getId() : null,
        initiative != null ? initiative.getName() : null,
        epic.getTeam().getId(),
        epic.getTeam().getName(),
        epic.getStoryPoints(),
        epic.getDueDate(),
        epic.getStatus());
  }
}
