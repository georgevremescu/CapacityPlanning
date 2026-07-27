package com.atoss.capacityplanning.config;

import com.atoss.capacityplanning.entity.Epic;
import com.atoss.capacityplanning.entity.EpicStatus;
import com.atoss.capacityplanning.entity.Initiative;
import com.atoss.capacityplanning.entity.InitiativeStatus;
import com.atoss.capacityplanning.entity.Person;
import com.atoss.capacityplanning.entity.Team;
import com.atoss.capacityplanning.repository.EpicRepository;
import com.atoss.capacityplanning.repository.InitiativeRepository;
import com.atoss.capacityplanning.repository.PersonRepository;
import com.atoss.capacityplanning.repository.TeamRepository;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Seeds a small demo dataset on first run so the prototype is immediately explorable.
// Skips seeding if data already exists (e.g. restarting against the persisted H2 file).
@Component
public class DataSeeder implements CommandLineRunner {

  private final TeamRepository teamRepository;
  private final PersonRepository personRepository;
  private final InitiativeRepository initiativeRepository;
  private final EpicRepository epicRepository;

  public DataSeeder(
      TeamRepository teamRepository,
      PersonRepository personRepository,
      InitiativeRepository initiativeRepository,
      EpicRepository epicRepository) {
    this.teamRepository = teamRepository;
    this.personRepository = personRepository;
    this.initiativeRepository = initiativeRepository;
    this.epicRepository = epicRepository;
  }

  @Override
  public void run(String... args) {
    if (teamRepository.count() > 0) {
      return;
    }

    Team platform = teamRepository.save(new Team("Platform"));
    Team mobile = teamRepository.save(new Team("Mobile"));
    Team data = teamRepository.save(new Team("Data"));

    // Overhead varies per person: e.g. Bob and Grace/Heidi carry heavier support-rotation
    // load than their teammates, so a single team-wide overhead number would understate
    // some people's real availability and overstate others'.
    personRepository.save(new Person("Alice Novak", platform, 1.0, 6.0, 0.10, 0.05));
    personRepository.save(new Person("Bob Fischer", platform, 0.8, 5.0, 0.15, 0.10));
    personRepository.save(new Person("Carol Weiss", platform, 1.0, 4.0, 0.10, 0.05));

    personRepository.save(new Person("Dave Kruger", mobile, 1.0, 5.0, 0.10, 0.05));
    personRepository.save(new Person("Eve Santos", mobile, 1.0, 5.5, 0.12, 0.08));

    personRepository.save(new Person("Frank Bauer", data, 0.6, 4.0, 0.10, 0.05));
    personRepository.save(new Person("Grace Lindqvist", data, 1.0, 6.0, 0.15, 0.10));
    personRepository.save(new Person("Heidi Moreau", data, 1.0, 5.0, 0.15, 0.10));

    Initiative checkoutRedesign =
        initiativeRepository.save(
            initiative(
                "Checkout Redesign",
                "Rework the checkout flow to reduce cart abandonment.",
                80,
                LocalDate.of(2026, 9, 30),
                InitiativeStatus.COMMITTED,
                1));

    Initiative offlineMode =
        initiativeRepository.save(
            initiative(
                "Offline Mode",
                "Allow the mobile app to function without connectivity.",
                120,
                LocalDate.of(2026, 12, 15),
                InitiativeStatus.PROPOSED,
                2));

    Initiative dataPlatformMigration =
        initiativeRepository.save(
            initiative(
                "Data Platform Migration",
                "Move analytics workloads onto the new warehouse.",
                150,
                LocalDate.of(2026, 11, 30),
                InitiativeStatus.COMMITTED,
                1));

    Initiative loyaltyProgram =
        initiativeRepository.save(
            initiative(
                "Loyalty Program",
                "Introduce points-based customer loyalty rewards.",
                60,
                LocalDate.of(2027, 1, 31),
                InitiativeStatus.PROPOSED,
                3));

    epicRepository.save(
        epic("Payment API revamp", checkoutRedesign, platform, 30, LocalDate.of(2026, 8, 15), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Checkout UI", checkoutRedesign, mobile, 25, LocalDate.of(2026, 9, 20), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Fraud checks", checkoutRedesign, platform, 20, LocalDate.of(2026, 9, 25), EpicStatus.IN_PROGRESS));

    epicRepository.save(
        epic("Offline banner & UX states", offlineMode, mobile, 15, LocalDate.of(2026, 10, 20), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Local cache layer", offlineMode, mobile, 40, LocalDate.of(2026, 11, 30), EpicStatus.PROPOSED));
    epicRepository.save(
        epic("Conflict resolution engine", offlineMode, platform, 25, LocalDate.of(2026, 11, 5), EpicStatus.IN_PROGRESS));
    epicRepository.save(
        epic("Sync engine", offlineMode, platform, 35, LocalDate.of(2026, 12, 10), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("Warehouse cutover", dataPlatformMigration, data, 60, LocalDate.of(2026, 10, 15), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("ETL rewrite", dataPlatformMigration, data, 50, LocalDate.of(2026, 11, 20), EpicStatus.IN_PROGRESS));
    epicRepository.save(
        epic("Legacy decommission", dataPlatformMigration, data, 40, LocalDate.of(2026, 12, 28), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("Points ledger", loyaltyProgram, data, 30, LocalDate.of(2027, 1, 15), EpicStatus.PROPOSED));
    epicRepository.save(
        epic("Loyalty UI", loyaltyProgram, mobile, 20, LocalDate.of(2027, 1, 20), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("Tech debt cleanup", null, platform, 15, LocalDate.of(2026, 8, 30), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Security audit fixes", null, data, 10, LocalDate.of(2026, 9, 5), EpicStatus.CANCELLED));
  }

  private static Initiative initiative(
      String name,
      String description,
      int estimatedStoryPoints,
      LocalDate targetDate,
      InitiativeStatus status,
      int priority) {
    Initiative initiative = new Initiative();
    initiative.setName(name);
    initiative.setDescription(description);
    initiative.setEstimatedStoryPoints(estimatedStoryPoints);
    initiative.setTargetDate(targetDate);
    initiative.setStatus(status);
    initiative.setPriority(priority);
    return initiative;
  }

  private static Epic epic(
      String name,
      Initiative initiative,
      Team team,
      int storyPoints,
      LocalDate dueDate,
      EpicStatus status) {
    Epic epic = new Epic();
    epic.setName(name);
    epic.setInitiative(initiative);
    epic.setTeam(team);
    epic.setStoryPoints(storyPoints);
    epic.setDueDate(dueDate);
    epic.setStatus(status);
    return epic;
  }
}
