# Capacity Planning Tool — Design Doc

Prototype for team-level capacity planning: how much work (in story points) each team
can take on in a quarter, versus how much is actually assigned to them, with a
committed-vs-proposed simulation toggle for trade-off discussions.

## Architecture

Single-server prototype. Spring Boot (embedded Tomcat) serves both the REST API and
the built React static files from one origin (`http://localhost:8080`). Data is
persisted to a file-based H2 database. There is no authentication — the tool assumes a
single implicit user (e.g. run locally by an EM/PM, or on a shared internal box with no
sensitive data).

```
Browser  <---->  Spring Boot (Tomcat, port 8080)
                  ├── /                  → static React build
                  └── /api/**            → REST API → JPA → H2 (file)
```

## Data Model

- **Team** — name, `overheadPercentage` (fraction of capacity lost to
  meetings/support/admin).
- **Person** — belongs to exactly one team; `availabilityFte` (0.0–1.0) and `velocity`
  (story points/working day), both constant over time.
- **Initiative** — top-down estimate (`estimatedStoryPoints`), `targetDate`, `status`
  (`PROPOSED`/`COMMITTED`/`CANCELLED`), optional `priority`. Teams "involved" in an
  initiative are *derived* from the teams of its child epics, not stored directly, to
  avoid the two going out of sync.
- **Epic** — the actual unit of planned work: `storyPoints`, `dueDate`, one `Team`, an
  optional `Initiative` (epics can stand alone), and its own `status`
  (`PROPOSED`/`COMMITTED`/`IN_PROGRESS`/`DONE`/`CANCELLED`).
- **Quarter** — not stored; derived from a date (`2026-Q3`) and used as the standard
  capacity bucket everywhere.

People are only ever created, read, updated, or deleted in the context of their team
(`/api/teams/{id}/people/**`) — there's no standalone person management, since a person
without a team doesn't mean anything in this model.

## Capacity Calculation

For a given team and quarter:

```
workingDaysInQuarter = 62                                    (constant, no holiday calendar)
teamRawCapacitySp     = Σ (person.availabilityFte * person.velocity * workingDaysInQuarter)
teamNetCapacitySp     = teamRawCapacitySp * (1 - team.overheadPercentage)
teamAllocatedSp       = Σ epic.storyPoints, for epics on that team whose dueDate falls in the quarter
utilization           = teamAllocatedSp / teamNetCapacitySp
```

This one calculation backs all three stakeholder views — the Executive Overview
aggregates it across teams, the Team Capacity view shows it for one team in detail.

`CANCELLED` epics are never counted as demand. Which of the remaining statuses count
depends on the simulation mode below.

## Simulation Toggle

Rather than a separate scenario/versioning system, the simulation feature reuses the
`status` field already on `Epic`:

- **Committed only** — counts epics with status `COMMITTED`, `IN_PROGRESS`, or `DONE`.
- **Include proposed (simulation)** — additionally counts `PROPOSED` epics.

This lets an EM/PM see "what if we said yes to this proposed initiative" by flipping
one toggle, without maintaining parallel named scenarios. The toggle lives in the app
header and drives both the Executive Overview and Team Capacity views.

Note: the calculation is driven entirely by *epic* status, not by the parent
initiative's status — an epic's own status is what represents its commitment level as
demand.

## REST API

```
GET/POST/PUT/DELETE   /api/teams
GET/POST/PUT/DELETE   /api/teams/{id}/people
GET/POST/PUT/DELETE   /api/initiatives          (GET /{id} returns epics + rollups + derived teams)
GET/POST/PUT/DELETE   /api/epics
GET  /api/capacity/teams/{teamId}?quarter=2026-Q3&mode=committed|simulated
GET  /api/capacity/overview?quarter=2026-Q3&mode=committed|simulated
```

## Frontend

Plain tab-based nav (no router needed for four flat views), state for the active tab,
selected quarter, and simulation mode lifted into `App.jsx` and passed down as props.
Views: Executive Overview, Initiatives (list + CRUD + detail), Epics (list + CRUD),
Team Capacity (roster CRUD, overhead editing, capacity-vs-allocated visual).

## Known Limitations / TODOs

These are intentional simplifications for a prototype, called out here and at the
relevant `// TODO:` in code:

- No authentication/authorization — single implicit user, no multi-tenancy.
- No audit trail, change history, or notifications.
- No real holiday calendar — working days per quarter is a hardcoded constant (62),
  same for every quarter and region.
- Person `availabilityFte` and `velocity` are constant over time — no modeling of
  leave, ramp-up/down, or historical-throughput-derived velocity. Velocity is a
  manually entered, rough self/manager estimate.
- A person belongs to exactly one team — no cross-team or partial allocation.
- Team `overheadPercentage` is a single blended number — not split into meeting
  overhead vs. support-load overhead.
- No formal prioritization algorithm — `Initiative.priority` is just a sortable field.
- No persisted, named simulation scenarios and no side-by-side scenario comparison —
  the simulation is a single global "committed vs. proposed" toggle.
- No pagination, search, sorting parameters, or bulk operations on any endpoint.
- No production deployment concerns — single embedded H2 file database, single JVM.
- Initiative status and its child epics' statuses are not cross-validated against each
  other; the capacity calculation only looks at epic status.
