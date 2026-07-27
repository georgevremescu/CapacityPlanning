import { useEffect, useState } from 'react';
import { capacityApi, epicsApi, initiativesApi, teamsApi } from '../api/index.js';
import { useAsync } from '../utils/useAsync.js';
import { quarterOfDateString } from '../utils/quarter.js';
import UtilizationBar from '../components/UtilizationBar.jsx';
import CrudForm from '../components/CrudForm.jsx';

const EPIC_STATUSES = ['PROPOSED', 'COMMITTED', 'IN_PROGRESS', 'DONE', 'CANCELLED'];

const EMPTY_PERSON_FORM = { name: '', availabilityFte: 1.0, velocity: 5.0 };

const PERSON_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  {
    name: 'availabilityFte',
    label: 'Availability (FTE)',
    type: 'number',
    min: 0,
    max: 1,
    step: 0.1,
    required: true,
  },
  { name: 'velocity', label: 'Velocity (sp/day)', type: 'number', min: 0, step: 0.5, required: true },
];

function OverheadEditor({ team, onSave }) {
  const [editing, setEditing] = useState(false);
  const [meetingValue, setMeetingValue] = useState(team.meetingOverheadPercentage);
  const [supportLoadValue, setSupportLoadValue] = useState(team.supportLoadOverheadPercentage);

  const total = team.meetingOverheadPercentage + team.supportLoadOverheadPercentage;

  if (!editing) {
    return (
      <span>
        {(total * 100).toFixed(0)}% ({(team.meetingOverheadPercentage * 100).toFixed(0)}% meetings +{' '}
        {(team.supportLoadOverheadPercentage * 100).toFixed(0)}% support){' '}
        <button
          className="link-button"
          onClick={() => {
            setMeetingValue(team.meetingOverheadPercentage);
            setSupportLoadValue(team.supportLoadOverheadPercentage);
            setEditing(true);
          }}
        >
          edit
        </button>
      </span>
    );
  }

  return (
    <span className="inline-edit">
      <label>
        Meetings{' '}
        <input
          type="number"
          min="0"
          max="1"
          step="0.01"
          value={meetingValue}
          onChange={(e) => setMeetingValue(e.target.value)}
        />
      </label>
      <label>
        Support load{' '}
        <input
          type="number"
          min="0"
          max="1"
          step="0.01"
          value={supportLoadValue}
          onChange={(e) => setSupportLoadValue(e.target.value)}
        />
      </label>
      <button
        onClick={async () => {
          await onSave(Number(meetingValue), Number(supportLoadValue));
          setEditing(false);
        }}
      >
        Save
      </button>
      <button className="button-secondary" onClick={() => setEditing(false)}>
        Cancel
      </button>
    </span>
  );
}

const EMPTY_EPIC_FORM = { name: '', initiativeId: '', storyPoints: 0, dueDate: '', status: 'PROPOSED' };

// initiativeId's options depend on the currently loaded initiatives list, so
// this is a function rather than a static constant like the other field sets.
function epicFields(initiatives) {
  return [
    { name: 'name', label: 'Name', type: 'text', required: true },
    {
      name: 'initiativeId',
      label: 'Initiative (optional)',
      type: 'select',
      nullable: true,
      nullableLabel: 'None (standalone)',
      numeric: true,
      options: initiatives.map((i) => ({ value: i.id, label: i.name })),
    },
    { name: 'storyPoints', label: 'Story Points', type: 'number', min: 0, step: 1, required: true },
    { name: 'dueDate', label: 'Due Date', type: 'date' },
    {
      name: 'status',
      label: 'Status',
      type: 'select',
      options: EPIC_STATUSES.map((s) => ({ value: s, label: s })),
    },
  ];
}

function byDueDate(a, b) {
  if (!a.dueDate && !b.dueDate) return 0;
  if (!a.dueDate) return 1;
  if (!b.dueDate) return -1;
  return a.dueDate.localeCompare(b.dueDate);
}

export default function TeamLeadView({ quarter, mode }) {
  const { data: teams, loading: teamsLoading, reload: reloadTeams } = useAsync(
    () => teamsApi.list(),
    [],
  );
  const [teamId, setTeamId] = useState(null);

  useEffect(() => {
    if (!teamId && teams && teams.length > 0) {
      setTeamId(teams[0].id);
    }
  }, [teams, teamId]);

  const {
    data: capacity,
    loading: capacityLoading,
    reload: reloadCapacity,
  } = useAsync(() => (teamId ? capacityApi.forTeam(teamId, quarter, mode) : Promise.resolve(null)), [
    teamId,
    quarter,
    mode,
  ]);

  const {
    data: people,
    loading: peopleLoading,
    reload: reloadPeople,
  } = useAsync(() => (teamId ? teamsApi.listPeople(teamId) : Promise.resolve([])), [teamId]);

  const {
    data: allEpics,
    loading: epicsLoading,
    reload: reloadEpics,
  } = useAsync(() => epicsApi.list(), []);

  const { data: initiatives } = useAsync(() => initiativesApi.list(), []);

  const [addingPerson, setAddingPerson] = useState(false);
  const [editingPersonId, setEditingPersonId] = useState(null);
  const [addingEpic, setAddingEpic] = useState(false);
  const [editingEpicId, setEditingEpicId] = useState(null);
  const [actionError, setActionError] = useState(null);

  const team = teams?.find((t) => t.id === teamId);
  const teamEpics = (allEpics?.filter((e) => e.teamId === teamId) ?? []).sort(byDueDate);

  const handleOverheadSave = async (meetingOverheadPercentage, supportLoadOverheadPercentage) => {
    try {
      await teamsApi.update(teamId, {
        ...team,
        meetingOverheadPercentage,
        supportLoadOverheadPercentage,
      });
      setActionError(null);
      reloadTeams();
      reloadCapacity();
    } catch (err) {
      setActionError(err.message);
    }
  };

  const handleAddPerson = async (payload) => {
    try {
      await teamsApi.addPerson(teamId, payload);
      setActionError(null);
      setAddingPerson(false);
      reloadPeople();
      reloadCapacity();
    } catch (err) {
      setActionError(err.message);
    }
  };

  const handleUpdatePerson = async (personId, payload) => {
    try {
      await teamsApi.updatePerson(teamId, personId, payload);
      setActionError(null);
      setEditingPersonId(null);
      reloadPeople();
      reloadCapacity();
    } catch (err) {
      setActionError(err.message);
    }
  };

  const handleDeletePerson = async (personId) => {
    if (!confirm('Remove this person from the team?')) return;
    try {
      await teamsApi.removePerson(teamId, personId);
      setActionError(null);
      reloadPeople();
      reloadCapacity();
    } catch (err) {
      setActionError(err.message);
    }
  };

  const handleCreateEpic = async (payload) => {
    try {
      await epicsApi.create(payload);
      setActionError(null);
      setAddingEpic(false);
      reloadEpics();
      reloadCapacity();
    } catch (err) {
      setActionError(err.message);
    }
  };

  const handleUpdateEpic = async (id, payload) => {
    try {
      await epicsApi.update(id, { ...payload, id });
      setActionError(null);
      setEditingEpicId(null);
      reloadEpics();
      reloadCapacity();
    } catch (err) {
      setActionError(err.message);
    }
  };

  const handleDeleteEpic = async (id) => {
    if (!confirm('Delete this epic?')) return;
    try {
      await epicsApi.remove(id);
      setActionError(null);
      reloadEpics();
      reloadCapacity();
    } catch (err) {
      setActionError(err.message);
    }
  };

  if (teamsLoading || !teams) return <p className="state-message">Loading teams...</p>;
  if (teams.length === 0) return <p className="state-message">No teams yet.</p>;

  return (
    <div className="view">
      <div className="view-header">
        <h2>Team Workspace</h2>
        <label className="control">
          <span>Team</span>
          <select value={teamId ?? ''} onChange={(e) => setTeamId(Number(e.target.value))}>
            {teams.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      {actionError && <p className="state-message state-message--error">{actionError}</p>}

      <div className="card">
        <h3>
          {team?.name} &middot; Overhead: {team && <OverheadEditor team={team} onSave={handleOverheadSave} />}
        </h3>
        {capacityLoading || !capacity ? (
          <p className="state-message">Loading capacity...</p>
        ) : (
          <>
            <UtilizationBar
              utilization={capacity.utilization}
              allocatedSp={capacity.allocatedSp}
              capacitySp={capacity.netCapacitySp}
            />
            <p className="metric-line">
              Raw capacity: {capacity.rawCapacitySp.toFixed(1)} sp &middot; Net capacity (after
              overhead): {capacity.netCapacitySp.toFixed(1)} sp &middot; {capacity.workingDaysInQuarter}{' '}
              working days assumed &middot; {quarter}
            </p>
          </>
        )}
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Roster</h3>
          <button onClick={() => setAddingPerson((v) => !v)}>
            {addingPerson ? 'Cancel' : 'Add Person'}
          </button>
        </div>
        {addingPerson && (
          <CrudForm
            inline
            fields={PERSON_FIELDS}
            initialValues={EMPTY_PERSON_FORM}
            onSubmit={handleAddPerson}
            onCancel={() => setAddingPerson(false)}
          />
        )}
        {peopleLoading || !people ? (
          <p className="state-message">Loading roster...</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Availability (FTE)</th>
                <th>Velocity (sp/day)</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {people.map((person) =>
                editingPersonId === person.id ? (
                  <tr key={person.id}>
                    <td colSpan={4}>
                      <CrudForm
                        inline
                        fields={PERSON_FIELDS}
                        initialValues={person}
                        onSubmit={(payload) => handleUpdatePerson(person.id, payload)}
                        onCancel={() => setEditingPersonId(null)}
                      />
                    </td>
                  </tr>
                ) : (
                  <tr key={person.id}>
                    <td>{person.name}</td>
                    <td>{(person.availabilityFte * 100).toFixed(0)}%</td>
                    <td>{person.velocity}</td>
                    <td className="table-actions">
                      <button
                        className="button-secondary"
                        onClick={() => setEditingPersonId(person.id)}
                      >
                        Edit
                      </button>
                      <button className="button-danger" onClick={() => handleDeletePerson(person.id)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ),
              )}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Epics</h3>
          <button onClick={() => setAddingEpic((v) => !v)} disabled={!teamId}>
            {addingEpic ? 'Cancel' : 'New Epic'}
          </button>
        </div>
        {addingEpic && initiatives && (
          <CrudForm
            fields={epicFields(initiatives)}
            initialValues={EMPTY_EPIC_FORM}
            onSubmit={(payload) => handleCreateEpic({ ...payload, teamId })}
            onCancel={() => setAddingEpic(false)}
          />
        )}
        {epicsLoading || !allEpics || !initiatives ? (
          <p className="state-message">Loading epics...</p>
        ) : teamEpics.length === 0 ? (
          <p className="state-message">No epics for this team yet.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Initiative</th>
                <th>Story Points</th>
                <th>Due Date</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {teamEpics.map((epic) =>
                editingEpicId === epic.id ? (
                  <tr key={epic.id}>
                    <td colSpan={6}>
                      <CrudForm
                        fields={epicFields(initiatives)}
                        initialValues={epic}
                        onSubmit={(payload) => handleUpdateEpic(epic.id, { ...payload, teamId })}
                        onCancel={() => setEditingEpicId(null)}
                      />
                    </td>
                  </tr>
                ) : (
                  <tr key={epic.id}>
                    <td>{epic.name}</td>
                    <td>{epic.initiativeName ?? 'Standalone'}</td>
                    <td>{epic.storyPoints}</td>
                    <td>
                      {epic.dueDate ?? '-'}
                      {epic.dueDate && quarterOfDateString(epic.dueDate) === quarter && (
                        <span className="badge badge--committed" style={{ marginLeft: 6 }}>
                          this quarter
                        </span>
                      )}
                    </td>
                    <td>
                      <span className={`badge badge--${epic.status.toLowerCase()}`}>
                        {epic.status}
                      </span>
                    </td>
                    <td className="table-actions">
                      <button className="button-secondary" onClick={() => setEditingEpicId(epic.id)}>
                        Edit
                      </button>
                      <button className="button-danger" onClick={() => handleDeleteEpic(epic.id)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ),
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
