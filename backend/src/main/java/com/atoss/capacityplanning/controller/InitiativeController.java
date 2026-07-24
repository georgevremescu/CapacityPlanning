package com.atoss.capacityplanning.controller;

import com.atoss.capacityplanning.dto.InitiativeDetailDto;
import com.atoss.capacityplanning.dto.InitiativeDto;
import com.atoss.capacityplanning.service.InitiativeService;
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
@RequestMapping("/api/initiatives")
public class InitiativeController {

  private final InitiativeService initiativeService;

  public InitiativeController(InitiativeService initiativeService) {
    this.initiativeService = initiativeService;
  }

  @GetMapping
  public List<InitiativeDto> findAll() {
    return initiativeService.findAll();
  }

  @GetMapping("/{id}")
  public InitiativeDetailDto findById(@PathVariable Long id) {
    return initiativeService.findByIdDetailed(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public InitiativeDto create(@RequestBody InitiativeDto request) {
    return initiativeService.create(request);
  }

  @PutMapping("/{id}")
  public InitiativeDto update(@PathVariable Long id, @RequestBody InitiativeDto request) {
    return initiativeService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    initiativeService.delete(id);
  }
}
