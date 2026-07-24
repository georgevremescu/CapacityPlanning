package com.atoss.capacityplanning.repository;

import com.atoss.capacityplanning.entity.Epic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpicRepository extends JpaRepository<Epic, Long> {

  List<Epic> findByInitiativeId(Long initiativeId);

  List<Epic> findByTeamId(Long teamId);
}
