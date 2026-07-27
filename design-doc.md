# Capacity Planning Tool — Design Doc

## Problem Statement

A prototype for team-level capacity planning, built for three named stakeholder
groups, each with a different question about the same underlying numbers:

* **Higher Management** — cross-team capacity, annual business-plan commitments, and
the impact of new (not-yet-committed) demand on the existing plan. Not interested in
per-team micromanagement detail.
* **Engineering Managers / Product Managers** — initiative delivery across teams,
trade-off decisions between competing initiatives, and running planning simulations
before anything is committed.
* **Team Leads / Product Owners** — their own team's roster, overhead, and epics; the
operational, single-team view.

The tool answers one question in every view: *for team X in quarter Y, how much work
capacity exists, and how much is actually assigned?* — aggregated and filtered
differently per audience.

## Development Process

Built end-to-end with AI assistance (Claude Code) from the very first line of code,
using an ask-plan-execute loop rather than one-shot generation:

1. **Plan** — the stakeholder brief was translated into a concrete implementation plan
(data model, API, build phases) before any code was written.
2. **Execute** — the plan was built out phase by phase (backend, frontend,
integration), verified in the browser and via the test suite at each step.
3. **Iterate** — follow-up sessions used AI for feature changes driven by direct
feedback (the persona-based UI redesign, build automation for the stale-bundle bug).
4. **Review \& refine** — a dedicated code-review pass, with the AI acting as a
principal-engineer reviewer of its own prior output, ranked findings by severity and
fixed them; further one-off fixes (`Locale.ROOT`, story-point typing, the shared
`CrudForm`) were requested and applied individually afterward.

## Architecture

Single-server prototype. Spring Boot (embedded Tomcat) serves both the REST API and
the built React static files from one origin (`http://localhost:8080`). Data is
persisted to a file-based H2 database. There is no authentication — the tool assumes a
single implicit user (an EM/PM running it locally, or on a shared internal box with no
sensitive data).

```
Browser  <---->  Spring Boot (Tomcat, port 8080)
                  ├── /                  → static React build (frontend-maven-plugin output)
                  └── /api/\*\*            → REST API → JPA → H2 (file, ./data/)
```

**Why single-server:** the explicit goal was a zero-friction local prototype — one URL,
one process, importable into Eclipse and run with one click. The cost (frontend and
backend can't be deployed/scaled independently, no CDN, no separate frontend release
cycle) was accepted deliberately for a prototype; it is called out below as something
to revisit before any real deployment.

**Build integration (`frontend-maven-plugin`):** the frontend build is wired directly
into Maven's `generate-resources` phase in `backend/pom.xml` — it downloads a local
Node/npm, runs `npm install`/`npm run build` in `frontend/`, and a
`maven-resources-plugin` step copies `frontend/dist` into
`backend/src/main/resources/static`, with `maven-clean-plugin` wiping stale hashed
assets first. An `m2e` lifecycle-mapping entry tells Eclipse to run this on explicit
builds but skip it on incremental keystroke builds.

**Lombok** was added to the four JPA entities (`@Getter`/`@Setter`) to remove
hand-written accessors. Maven/javac handles Lombok's annotation processing natively;

## Data Model

* **Team** — just `name`. Team is a grouping/reporting boundary; it carries no capacity
math of its own — that all lives on the people in it.
* **Person** — belongs to exactly one team; `availabilityFte` (0.0–1.0), `velocity`
(story points/working day, `double` — a genuinely fractional rate),
`meetingOverheadPercentage` (fraction of *this person's* time lost to recurring
meetings/admin), and `supportLoadOverheadPercentage` (fraction lost to ad-hoc
support/interrupt work) — all four constant over time, all four entered per person.
* **Initiative** — top-down estimate `estimatedStoryPoints` (**`int`**, not `double` —
see below), `targetDate`, `status` (`PROPOSED`/`COMMITTED`/`CANCELLED`), optional
`priority`. Teams "involved" in an initiative are *derived* from the teams of its
child epics, not stored directly, to avoid the two going out of sync.
* **Epic** — the actual unit of planned work: `storyPoints` (**`int`**), `dueDate`, one
`Team`, an optional `Initiative` (epics can stand alone), and its own `status`
(`PROPOSED`/`COMMITTED`/`IN\_PROGRESS`/`DONE`/`CANCELLED`).
* **Quarter** — not stored; derived from a date (`2026-Q3`) and used as the standard
capacity bucket everywhere.

People are only ever created, read, updated, or deleted in the context of their team
(`/api/teams/{id}/people/\*\*`) — there's no standalone person management.

## Entities \& Attributes Added Beyond the Original Brief

**Added entity**

* **Quarter** (not persisted, derived from any date) — Quarter was chosen as the bucket
size for every calculation and view because it sits between an exact due date and a
full year and is the conventional cadence for capacity planning; nothing in the brief
mandates this specifically.

**Added attributes**

* **`Person.velocity`** (story points/working day) — the brief gives Person no
attributes at all. Added because "estimated effort" (Initiative) and "estimate"
(Epic) needed a matching capacity-side unit to be compared against; without a
per-person throughput rate, `availabilityFte` alone can't be turned into a
story-point capacity figure.
* **`Initiative.status`** (`PROPOSED`/`COMMITTED`/`CANCELLED`) and **`Epic.status`**
(`PROPOSED`/`COMMITTED`/`IN\_PROGRESS`/`DONE`/`CANCELLED`) — invented as the entire
mechanism behind "planning simulations before commitments are made" (EM/PM) and
"impact of new demands" (Higher Management). The two enums are not cross-validated
against each other, and only `Epic.status` actually feeds the capacity math (see
Open Questions and Known Limitations).
* **`Initiative.priority`** (optional) — invented to give EM/PM's "trade-off decisions"
something concrete to rank by.
* **`Initiative.description`** — plain usability addition (free-text notes); not tied to
any stated requirement.
* **Unit choice: story points.** Both Initiative "estimated effort" and Epic "estimate"
were interpreted as Scrum-style story points. This choice is what makes
`Person.velocity` necessary in the first place; a different unit choice (person-days,
t-shirt sizes) would have implied a different Person attribute.

**Modeling decision, not a new field:** "teams involved in an initiative" is *derived*
from the teams of its child epics rather than stored as a direct relationship, to avoid
a duplicate/driftable field. The tradeoff: an initiative with no epics yet has no
derivable teams — and, per the Assumptions below, no capacity impact either.

## Capacity Calculation

For a given team and quarter (`CapacityService.calculate`):

```
workingDaysInQuarter = 62                                    (constant, no holiday calendar)
personRawCapacitySp   = person.availabilityFte \* person.velocity \* workingDaysInQuarter
personNetCapacitySp   = personRawCapacitySp \* (1 - (person.meetingOverheadPercentage + person.supportLoadOverheadPercentage))
teamRawCapacitySp     = Σ personRawCapacitySp, over the team's roster
teamNetCapacitySp     = Σ personNetCapacitySp, over the team's roster
teamAllocatedSp       = Σ epic.storyPoints, for epics on that team whose dueDate falls in the quarter,
                        filtered by status (see Simulation Toggle below)
utilization           = teamAllocatedSp / teamNetCapacitySp   (0 if netCapacitySp is 0)
```

Overhead is applied per person, then summed — not applied once to the team's raw total —
because it varies by individual (someone carrying more of the support rotation has a
different net contribution than a teammate at the same availability/velocity but less
support load). A team-wide blended percentage would silently misstate both people's real
capacity.

`CANCELLED` epics never count as demand, in either mode. This one calculation backs
every view; only the aggregation differs per audience (org totals + per-team side by
side for Higher Management; per-team-involved-in-an-initiative for EM/PM; single team
in detail for Team Lead/PO).

## Simulation Toggle

Rather than a separate scenario/versioning system, simulation reuses the `status`
field already on `Epic`:

* **Committed** — counts epics with status `COMMITTED`, `IN\_PROGRESS`, or `DONE`.
* **Simulated** — additionally counts `PROPOSED` epics.

The calculation is driven entirely by *epic* status, not the parent initiative's
status — an initiative can be `PROPOSED` while some of its epics are already
`COMMITTED`; the capacity math only looks at the epic.

## Frontend — Persona-Based Navigation

Three tabs, one per stakeholder, **not** one per data entity:

* **Capacity Outlook** (Higher Management) — org totals and per-team utilization shown
as **committed vs. simulated side by side**, with no toggle (the header's simulation
toggle is hidden on this tab — see `Layout.jsx`), since "impact of new demand" should
be visible without an interaction. Includes a 4-quarter annual outlook for the
committed plan.
* **Planning Simulation** (EM/PM) — initiatives sorted by `priority` (nulls last).
Opening an initiative shows its rolled-up epic story points (bottom-up, vs. the
initiative's own top-down estimate), its derived list of involved teams, and each
involved team's live capacity for the selected quarter/mode.
* **Team Workspace** (Team Lead/PO) — scoped to one team via a dropdown: roster CRUD
(each person's availability, velocity, meeting overhead, and support-load overhead all
editable inline), and full epic CRUD (with an implicit team — no team dropdown in the
epic form, since it's contextual to whichever team tab is selected).

## Visual Design Rationale

Most visual choices followed from constraints, not aesthetic preference:

* **Color carries meaning; nothing else competes with it.** The plan explicitly
required "per-team utilization bars (red/yellow/green)" — a business requirement,
not a palette choice. That's the only place color is semantic: `<85%` green
(`--healthy`), `85–100%` amber (`--warning`, a conventional "pay attention" cutoff in
capacity planning), `>100%` red (`--danger`). Everything else is neutral text/
background plus exactly one accent color (indigo `#4f46e5`) for interactive elements,
so the traffic-light signal isn't drowned out by other colorful UI.
* **Dark mode came nearly free.** Every color was a CSS custom property from the start
(`--text`, `--bg`, `--surface`, `--border`), so a second value set under
`prefers-color-scheme: dark` cost a handful of overrides, not a parallel stylesheet —
worth it for a tool likely to sit open next to an IDE.
* **Cards + plain tables, not a KPI-tile dashboard.** Every view is fundamentally rows
of tabular data (people, epics, teams); bordered "card" containers group that data,
with plain HTML tables inside rather than forcing it into dashboard tiles. Status
badges (colored pills for `PROPOSED`/`COMMITTED`/etc.) are the one Jira/Linear-style
convention added on top, since status is scanned repeatedly across many rows.
* **Tabs, not routes** — the build plan explicitly said tab-based nav was enough for a
prototype and React Router was optional, so navigation is local React state, and
shared controls (quarter selector, simulation toggle) live once in the header instead
of duplicated per view.
* **No component library** — hand-written CSS instead of Tailwind/MUI/Ant, so the
prototype stays something anyone (including someone opening it cold in Eclipse) can
read and tweak without an extra dependency/learning curve.
* **Layout** (single centered column, capped at 1100px, cards stacked vertically) was
the least deliberate choice — a sane default for table readability. The one
responsive grid (Capacity Outlook's committed-vs-simulated comparison) uses
`auto-fit`/`minmax` so it degrades to stacked on narrow viewports without media
queries.

## REST API

```
GET/POST/PUT/DELETE   /api/teams
GET/POST/PUT/DELETE   /api/teams/{id}/people
GET/POST/PUT/DELETE   /api/initiatives          (GET /{id} returns epics + rollups + derived teams)
GET/POST/PUT/DELETE   /api/epics
GET  /api/capacity/teams/{teamId}?quarter=2026-Q3\&mode=committed|simulated
GET  /api/capacity/overview?quarter=2026-Q3\&mode=committed|simulated
```

Bean Validation (`@NotBlank`/`@NotNull`/`@PositiveOrZero`/`@DecimalMin`/`@DecimalMax`)
is wired to all four DTOs via `@Valid` on the controllers, with
`MethodArgumentNotValidException` mapped to a `400` with a joined field-error message.
This closes a real gap: `spring-boot-starter-validation` was a declared dependency with
zero annotations anywhere until the code-review pass — nothing had stopped
`meetingOverheadPercentage`/`supportLoadOverheadPercentage > 1` individually, negative
`storyPoints`/`velocity`, or blank names from being persisted via a direct API call (the
frontend's `min`/`max` on `<input type="number">` was UI-only, not enforcement).
`TeamService.addPerson`/`updatePerson` additionally reject a person's two overhead
fields summing above `1.0`, since Bean Validation can only constrain each field
independently.

`DataIntegrityViolationException` (an FK constraint violation) is mapped globally to a
`409 Conflict`, as a safety net for any delete that still has dependent rows.

## Architecture Decisions \& Reasoning (summary)

|Decision|Reasoning|
|-|-|
|Single Spring Boot server, embedded Tomcat, serves API + static build|Zero-friction local run; one URL, one process, Eclipse-importable|
|H2 file DB, no auth|Single implicit local user; not a production concern for this prototype|
|Simulation via `Epic.status`, not a separate scenario system|Reuses data already modeled; avoids maintaining parallel/branching plan data|
|Persona-based tabs, not entity-based tabs|Focused on who needs what|
|`frontend-maven-plugin` bound to Maven lifecycle|Binding to the build makes it impossible to forget|
|`CrudForm` shared component|Needed once a third near-identical form existed — avoided premature abstraction at 2 forms|
|Lombok for entity accessors|Less boilerplate|
|Bean Validation wired to all write DTOs|Dependency was already present but unused; API had no input enforcement beyond client-side hints|
|Git repo created only after the prototype worked end-to-end|Original plan explicitly said no git init; the user asked for it later, once satisfied with the app|

## Known Limitations / TODOs (still open, deliberate)

* No authentication/authorization, no multi-tenancy, no audit trail, no notifications.
* No real holiday calendar — working days per quarter is a hardcoded constant (62).
* `availabilityFte`/`velocity`/`meetingOverheadPercentage`/`supportLoadOverheadPercentage`
are all constant over time — no leave/ramp modeling, no throughput-derived velocity,
no seasonal meeting/support load (e.g. an on-call rotation week).
* A person belongs to exactly one team — no cross-team or partial allocation.
* No formal prioritization algorithm — `Initiative.priority` is just a sortable field.
* No persisted, named simulation scenarios — a single global committed/proposed toggle.
* No pagination, search, sorting, or bulk operations on any endpoint.
* No production deployment concerns — single embedded H2 file DB, single JVM.
* Initiative status is not cross-validated against its child epics' statuses; the
capacity calculation only ever looks at epic-level status.
* Raw browser `confirm()` dialogs used for delete confirmation (acceptable for a local
prototype, not production UX).

## Open Questions

1. **Team delete vs. Initiative delete are inconsistent.** Deleting an Initiative
silently unlinks its child epics (`epic.initiative = null`) before deleting, matching
the UI's own "Its epics will remain, unlinked" copy. Deleting a Team with people or
epics still attached has no equivalent unlink step — it will hit the FK constraint
and now surfaces as a generic `409 Conflict` ("Cannot complete this action: other
records still reference it") instead of a raw `500`. That's safe, but the UI
currently has no way to trigger Team delete at all, so whether it *should* cascade
like Initiative delete, or stay a hard block, was never decided.
2. **Field-level validation errors aren't rendered per-field.** The backend now returns
a joined string of all failing fields on a `400` (e.g. `"name must not be blank; storyPoints must be greater than or equal to 0"`), but `CrudForm` and the views that
use it (`EmPmView`, `TeamLeadView`) only show this as one flat banner string, not
inline next to the offending field. The transcript that introduced `CrudForm`
explicitly flagged this as the trigger for revisiting the abstraction ("the moment
... field-level validation errors need to render consistently across forms") but
that revisit hasn't happened yet.
3. **No named/persisted simulation scenarios** — still a single global toggle; if more
than one hypothetical needs comparing side by side, this will need real design work.

## Assumptions Made (requirements were ambiguous or unrecoverable)

* **Quarter as the planning bucket.** The brief specifies no time granularity; quarter
was assumed to be the right bucket size for every calculation and view, with a
hardcoded 62-working-days-per-quarter constant invented to make the arithmetic work.
* **"Estimated effort"/"estimate" assumed to mean story points.** The brief never names
a unit; story points (and a matching per-person `velocity` in points/day) were
assumed as the common unit connecting demand (Initiative/Epic estimates) to supply
(team capacity).
* **Initiative-level estimates assumed not to count until broken into epics.**
`Initiative.estimatedStoryPoints` is stored for top-down/bottom-up comparison but
never enters the capacity calculation — only `Epic.storyPoints` does. This means a
newly proposed, not-yet-decomposed initiative shows zero capacity impact to Higher
Management, even though "impact of new demands" is their stated concern; the
assumption is that this is acceptable because the brief also says the tool operates
"at initiative and epic level," which was read as epics being where impact is
actually measured.
* **"Cross-team capacity" assumed to mean org-aggregate, not a team-by-team
breakdown.** The brief's wording could support either reading; a per-team table
existed in the first build but was removed at the user's explicit request ("I am not
doing micromanagement") mid-project. That live instruction was taken as authoritative
over the brief's literal phrase.
* **"Planning simulations" and "trade-off decisions" assumed satisfiable with one
global toggle.** No per-initiative or per-scenario isolation was built; running the
simulation means literally every `PROPOSED` epic org-wide counts, not just a specific
initiative's. `Initiative.priority` answers "trade-off decisions" partially (ranking),
but isolated what-if comparison was assumed out of scope for a prototype.
* **Stakeholder groups assumed to be view groupings, not access-control boundaries.**
The brief describes three roles with different concerns, not different permissions;
the tool assumes anyone can reach any tab and mutate any data, with no enforcement
tying, e.g., roster edits to a "Team Lead" identity.

