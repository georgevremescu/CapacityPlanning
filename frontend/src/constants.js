// Query-param values accepted by /api/capacity/**; must match backend CapacityMode.
export const CAPACITY_MODE = {
  COMMITTED: 'committed',
  SIMULATED: 'simulated',
};

// Tab ids driving which view App.jsx renders; must match Layout.jsx's tab definitions.
export const TAB = {
  HIGHER_MANAGEMENT: 'higher-management',
  EM_PM: 'em-pm',
  TEAM_LEAD: 'team-lead',
};

// Mirrors the backend EpicStatus enum.
export const EPIC_STATUS = {
  PROPOSED: 'PROPOSED',
  COMMITTED: 'COMMITTED',
  IN_PROGRESS: 'IN_PROGRESS',
  DONE: 'DONE',
  CANCELLED: 'CANCELLED',
};
export const EPIC_STATUSES = Object.values(EPIC_STATUS);

// Mirrors the backend InitiativeStatus enum.
export const INITIATIVE_STATUS = {
  PROPOSED: 'PROPOSED',
  COMMITTED: 'COMMITTED',
  CANCELLED: 'CANCELLED',
};
export const INITIATIVE_STATUSES = Object.values(INITIATIVE_STATUS);
