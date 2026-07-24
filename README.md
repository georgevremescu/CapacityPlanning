# Capacity Planning Tool

A prototype for team-level capacity planning: how much work each team can take on in a
quarter, versus how much is actually assigned, with a committed-vs-proposed simulation
toggle for trade-off discussions.

See [design-doc.md](design-doc.md) for the data model, capacity calculation, and known
limitations.

## Prerequisites

- JDK 21
- Maven 3.9+
- Node.js 20+ and npm

## Project Layout

```
backend/    Spring Boot (Maven) — REST API + serves the built frontend
frontend/   React (Vite) — the UI
```

## Running for local development

Run backend and frontend as two separate processes; Vite proxies `/api` calls to the
backend so both hot-reload independently.

**Backend** (from `backend/`):

```
mvn spring-boot:run
```

Starts on `http://localhost:8080`. On first run it seeds a small demo dataset (a few
teams, people, initiatives, and epics) into a local H2 file database at
`backend/data/`.

**Frontend** (from `frontend/`):

```
npm install
npm run dev
```

Starts on `http://localhost:5173` (or similar), proxying `/api/**` to
`http://localhost:8080`.

## Running as a single server (prototype "production" mode)

The backend's Maven build (`frontend-maven-plugin`, see `backend/pom.xml`) builds the
frontend and copies it into the backend's static resources automatically, so Spring
Boot serves everything from one URL:

```
cd backend
mvn spring-boot:run
```

The first run downloads a local Node/npm under `frontend/` and runs `npm install` +
`npm run build` as part of the `generate-resources` phase — no manual build/copy step
needed. Open `http://localhost:8080` — the API and UI are both served from there.

## Running Tests

**Backend** (from `backend/`) — JUnit 5 + Mockito unit tests for the service layer
(capacity calculation, CRUD services) and the `Quarter` date-math utility:

```
mvn test
```

**Frontend** (from `frontend/`) — Vitest tests for the pure quarter-math utilities:

```
npm test
```

## Importing into Eclipse

1. `File > Import... > Maven > Existing Maven Projects`.
2. Browse to `backend/` and select the `pom.xml`. Finish the import.
3. Eclipse will resolve dependencies via Maven; wait for the build to complete.
4. Use `Run As > Maven build...` with goal `spring-boot:run` (or `package`/`install`)
   so Maven's lifecycle — and with it the frontend build — actually runs. Running
   `CapacityPlanningApplication.java` directly as a Java Application skips the Maven
   lifecycle entirely and will serve whatever static files already happen to be under
   `src/main/resources/static` (possibly stale or missing).
5. The frontend is not an Eclipse project (no `.project`/`.classpath` — it's a separate
   npm/Vite project, not something Eclipse's Java tooling understands), so you won't
   see it in Package Explorer. Its build is wired into the backend's Maven lifecycle
   (previous point) instead. For frontend-only iteration with hot reload, run
   `npm run dev` separately against the Eclipse-launched backend.
6. Entities use Lombok (`@Getter`/`@Setter`) to generate accessors at compile time.
   Maven builds work out of the box, but Eclipse's own editor/compiler needs the
   Lombok jar installed once so it recognizes the generated methods (otherwise you'll
   see red squiggly "method does not exist" errors that don't actually affect the
   build): locate `lombok-*.jar` under your local `~/.m2/repository/org/projectlombok/lombok/`,
   run `java -jar lombok-<version>.jar`, and point the installer at your Eclipse
   installation. Restart Eclipse afterward.

## Notes

- Database: file-based H2 at `backend/data/capacityplanning.mv.db`. Delete that file
  (or the `backend/data/` folder) to reset to a fresh seeded state.
- No authentication — this is a single-user local prototype.
