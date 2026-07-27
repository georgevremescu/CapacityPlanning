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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Seeds a demo dataset on first run so the prototype is immediately explorable.
// Skips seeding if data already exists (e.g. restarting against the persisted H2 file).
@Component
public class DataSeeder implements CommandLineRunner {

  // Platform/Mobile/Data keep their hand-authored engineers (below) and are topped up
  // with generated ones to these sizes; team sizes are deliberately uneven (5-18) rather
  // than an even split, to reflect that real orgs don't staff every team identically.
  private static final int PLATFORM_TARGET_SIZE = 18;
  private static final int MOBILE_TARGET_SIZE = 11;
  private static final int DATA_TARGET_SIZE = 13;

  private record TeamSpec(String name, int size) {}

  // Sizes sum to 108; combined with Platform/Mobile/Data above (18+11+13=42) that's
  // 150 engineers across 15 teams in total.
  private static final List<TeamSpec> ADDITIONAL_TEAM_SPECS =
      List.of(
          new TeamSpec("Payments", 5),
          new TeamSpec("Growth", 6),
          new TeamSpec("Search", 6),
          new TeamSpec("Infrastructure", 7),
          new TeamSpec("Security", 8),
          new TeamSpec("Identity", 8),
          new TeamSpec("Analytics", 9),
          new TeamSpec("Machine Learning", 9),
          new TeamSpec("Design Systems", 10),
          new TeamSpec("Developer Experience", 10),
          new TeamSpec("Checkout", 12),
          new TeamSpec("Notifications", 18));

  // Combined with LAST_NAMES (15 * 10 = 150 combinations) to generate unique names for
  // the bulk-generated roster, without needing a random/non-reproducible seed.
  private static final String[] FIRST_NAMES = {
    "Ivan", "Julia", "Kevin", "Laura", "Mike", "Nina", "Oscar", "Paula", "Quinn", "Rita",
    "Sam", "Tina", "Uma", "Victor", "Wendy"
  };
  private static final String[] LAST_NAMES = {
    "Andersen", "Brandt", "Costa", "Duarte", "Ellison", "Farkas", "Gomez", "Haddad", "Ibrahim", "Jansen"
  };

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

    // Top up Platform/Mobile/Data to their target sizes, then create and staff the
    // remaining teams - all with generated engineers, continuing the same name index so
    // nobody in the org shares a name.
    int nextNameIndex = 0;
    nextNameIndex = seedGeneratedEngineers(platform, PLATFORM_TARGET_SIZE - 3, nextNameIndex);
    nextNameIndex = seedGeneratedEngineers(mobile, MOBILE_TARGET_SIZE - 2, nextNameIndex);
    nextNameIndex = seedGeneratedEngineers(data, DATA_TARGET_SIZE - 3, nextNameIndex);
    Map<String, Team> additionalTeams = new LinkedHashMap<>();
    for (TeamSpec spec : ADDITIONAL_TEAM_SPECS) {
      Team team = teamRepository.save(new Team(spec.name()));
      nextNameIndex = seedGeneratedEngineers(team, spec.size(), nextNameIndex);
      additionalTeams.put(spec.name(), team);
    }
    Team payments = additionalTeams.get("Payments");
    Team growth = additionalTeams.get("Growth");
    Team search = additionalTeams.get("Search");
    Team infrastructure = additionalTeams.get("Infrastructure");
    Team security = additionalTeams.get("Security");
    Team identity = additionalTeams.get("Identity");
    Team analytics = additionalTeams.get("Analytics");
    Team machineLearning = additionalTeams.get("Machine Learning");
    Team designSystems = additionalTeams.get("Design Systems");
    Team developerExperience = additionalTeams.get("Developer Experience");
    Team checkout = additionalTeams.get("Checkout");
    Team notifications = additionalTeams.get("Notifications");

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

    Initiative searchRelevanceOverhaul =
        initiativeRepository.save(
            initiative(
                "Search Relevance Overhaul",
                "Improve query understanding and ranking quality.",
                70,
                LocalDate.of(2026, 10, 31),
                InitiativeStatus.COMMITTED,
                2));

    Initiative zeroTrustAccessRollout =
        initiativeRepository.save(
            initiative(
                "Zero Trust Access Rollout",
                "Move internal access control to per-request verification.",
                90,
                LocalDate.of(2026, 11, 15),
                InitiativeStatus.COMMITTED,
                1));

    Initiative mlPlatformFoundations =
        initiativeRepository.save(
            initiative(
                "ML Platform Foundations",
                "Shared feature store and model registry for ML workloads.",
                100,
                LocalDate.of(2027, 1, 15),
                InitiativeStatus.PROPOSED,
                2));

    Initiative designSystem2 =
        initiativeRepository.save(
            initiative(
                "Design System 2.0",
                "Rebuild the shared component library and its tooling.",
                50,
                LocalDate.of(2026, 10, 15),
                InitiativeStatus.COMMITTED,
                3));

    Initiative notificationReliability =
        initiativeRepository.save(
            initiative(
                "Notification Reliability",
                "Reduce dropped/delayed notifications across providers.",
                40,
                LocalDate.of(2026, 9, 20),
                InitiativeStatus.COMMITTED,
                1));

    Initiative paymentsExpansion =
        initiativeRepository.save(
            initiative(
                "Payments Expansion",
                "Support additional currencies and a faster checkout path.",
                90,
                LocalDate.of(2027, 1, 10),
                InitiativeStatus.PROPOSED,
                2));

    Initiative growthExperimentsPlatform =
        initiativeRepository.save(
            initiative(
                "Growth Experiments Platform",
                "Self-serve A/B testing for the growth team.",
                45,
                LocalDate.of(2027, 1, 20),
                InitiativeStatus.PROPOSED,
                3));

    Initiative infraModernization =
        initiativeRepository.save(
            initiative(
                "Infra Modernization",
                "Migrate remaining workloads off the legacy cluster.",
                60,
                LocalDate.of(2026, 10, 31),
                InitiativeStatus.COMMITTED,
                1));

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
        epic("Query understanding revamp", searchRelevanceOverhaul, search, 25, LocalDate.of(2026, 9, 10), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Ranking model v2", searchRelevanceOverhaul, search, 30, LocalDate.of(2026, 10, 25), EpicStatus.IN_PROGRESS));
    epicRepository.save(
        epic("Personalized results", searchRelevanceOverhaul, search, 20, LocalDate.of(2026, 12, 20), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("SSO enforcement", zeroTrustAccessRollout, security, 30, LocalDate.of(2026, 9, 15), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Device posture checks", zeroTrustAccessRollout, security, 25, LocalDate.of(2026, 11, 10), EpicStatus.IN_PROGRESS));
    epicRepository.save(
        epic("Credential rotation automation", zeroTrustAccessRollout, identity, 15, LocalDate.of(2026, 8, 20), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Just-in-time access", zeroTrustAccessRollout, identity, 20, LocalDate.of(2026, 9, 25), EpicStatus.PROPOSED));
    epicRepository.save(
        epic("Security incident response automation", zeroTrustAccessRollout, security, 20, LocalDate.of(2026, 12, 15), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("Model registry", mlPlatformFoundations, machineLearning, 25, LocalDate.of(2026, 9, 30), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Feature store MVP", mlPlatformFoundations, machineLearning, 35, LocalDate.of(2026, 11, 30), EpicStatus.PROPOSED));
    epicRepository.save(
        epic("Analytics event pipeline hardening", mlPlatformFoundations, analytics, 20, LocalDate.of(2026, 9, 8), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Experiment tracking dashboards", mlPlatformFoundations, analytics, 20, LocalDate.of(2026, 12, 5), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("Token pipeline rewrite", designSystem2, designSystems, 20, LocalDate.of(2026, 9, 5), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Component accessibility audit", designSystem2, designSystems, 15, LocalDate.of(2026, 10, 10), EpicStatus.IN_PROGRESS));
    epicRepository.save(
        epic("Theming API v2", designSystem2, designSystems, 15, LocalDate.of(2026, 12, 10), EpicStatus.PROPOSED));
    epicRepository.save(
        epic("CLI scaffolding tool", designSystem2, developerExperience, 15, LocalDate.of(2026, 9, 25), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Docs site relaunch", designSystem2, developerExperience, 15, LocalDate.of(2026, 11, 15), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("Delivery retry pipeline", notificationReliability, notifications, 20, LocalDate.of(2026, 9, 18), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Push provider failover", notificationReliability, notifications, 20, LocalDate.of(2026, 10, 30), EpicStatus.IN_PROGRESS));
    epicRepository.save(
        epic("In-app notification center", notificationReliability, notifications, 25, LocalDate.of(2026, 12, 12), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("Refund automation", paymentsExpansion, payments, 20, LocalDate.of(2026, 9, 22), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Multi-currency support", paymentsExpansion, payments, 30, LocalDate.of(2026, 12, 1), EpicStatus.PROPOSED));
    epicRepository.save(
        epic("Express checkout button", paymentsExpansion, checkout, 25, LocalDate.of(2026, 11, 25), EpicStatus.IN_PROGRESS));
    epicRepository.save(
        epic("Saved payment methods", paymentsExpansion, checkout, 20, LocalDate.of(2026, 12, 18), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("Referral program v1", growthExperimentsPlatform, growth, 20, LocalDate.of(2026, 9, 28), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("A/B testing framework", growthExperimentsPlatform, growth, 25, LocalDate.of(2026, 11, 15), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("CI pipeline speedups", infraModernization, infrastructure, 25, LocalDate.of(2026, 9, 12), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Kubernetes migration", infraModernization, infrastructure, 35, LocalDate.of(2026, 10, 20), EpicStatus.IN_PROGRESS));
    epicRepository.save(
        epic("Multi-region failover", infraModernization, infrastructure, 30, LocalDate.of(2026, 12, 22), EpicStatus.PROPOSED));

    epicRepository.save(
        epic("Tech debt cleanup", null, platform, 15, LocalDate.of(2026, 8, 30), EpicStatus.COMMITTED));
    epicRepository.save(
        epic("Security audit fixes", null, data, 10, LocalDate.of(2026, 9, 5), EpicStatus.CANCELLED));
  }

  // Generates `count` engineers for `team`, named and sized deterministically from
  // `startIndex` so repeated calls never collide on a name. Returns the next free index.
  private int seedGeneratedEngineers(Team team, int count, int startIndex) {
    for (int i = 0; i < count; i++) {
      int idx = startIndex + i;
      String name =
          FIRST_NAMES[idx % FIRST_NAMES.length]
              + " "
              + LAST_NAMES[(idx / FIRST_NAMES.length) % LAST_NAMES.length];
      double availabilityFte = round(0.6 + 0.1 * (idx % 5)); // 0.6-1.0
      double velocity = round(3.0 + 0.5 * (idx % 9)); // 3.0-7.0 sp/day
      double meetingOverheadPercentage = round(0.05 + 0.01 * (idx % 11)); // 5%-15%
      double supportLoadOverheadPercentage = round(0.03 + 0.01 * (idx % 8)); // 3%-10%
      personRepository.save(
          new Person(
              name, team, availabilityFte, velocity, meetingOverheadPercentage, supportLoadOverheadPercentage));
    }
    return startIndex + count;
  }

  private static double round(double value) {
    return Math.round(value * 100.0) / 100.0;
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
