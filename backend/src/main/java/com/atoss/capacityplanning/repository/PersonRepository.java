package com.atoss.capacityplanning.repository;

import com.atoss.capacityplanning.entity.Person;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

  List<Person> findByTeamId(Long teamId);
}
