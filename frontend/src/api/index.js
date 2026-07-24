import { client } from './client.js';

export const teamsApi = {
  list: () => client.get('/teams'),
  get: (id) => client.get(`/teams/${id}`),
  create: (team) => client.post('/teams', team),
  update: (id, team) => client.put(`/teams/${id}`, team),
  remove: (id) => client.delete(`/teams/${id}`),

  listPeople: (teamId) => client.get(`/teams/${teamId}/people`),
  addPerson: (teamId, person) => client.post(`/teams/${teamId}/people`, person),
  updatePerson: (teamId, personId, person) =>
    client.put(`/teams/${teamId}/people/${personId}`, person),
  removePerson: (teamId, personId) => client.delete(`/teams/${teamId}/people/${personId}`),
};

export const initiativesApi = {
  list: () => client.get('/initiatives'),
  get: (id) => client.get(`/initiatives/${id}`),
  create: (initiative) => client.post('/initiatives', initiative),
  update: (id, initiative) => client.put(`/initiatives/${id}`, initiative),
  remove: (id) => client.delete(`/initiatives/${id}`),
};

export const epicsApi = {
  list: () => client.get('/epics'),
  get: (id) => client.get(`/epics/${id}`),
  create: (epic) => client.post('/epics', epic),
  update: (id, epic) => client.put(`/epics/${id}`, epic),
  remove: (id) => client.delete(`/epics/${id}`),
};

export const capacityApi = {
  forTeam: (teamId, quarter, mode) =>
    client.get(`/capacity/teams/${teamId}?quarter=${quarter}&mode=${mode}`),
  overview: (quarter, mode) => client.get(`/capacity/overview?quarter=${quarter}&mode=${mode}`),
};
