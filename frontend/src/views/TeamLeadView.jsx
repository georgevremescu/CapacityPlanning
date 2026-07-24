import { useEffect, useState } from 'react';
import { capacityApi, epicsApi, initiativesApi, teamsApi } from '../api/index.js';
import { useAsync } from '../utils/useAsync.js';
import { quarterOfDateString } from '../utils/quarter.js';
import UtilizationBar from '../components/UtilizationBar.jsx';

const EPIC_STATUSES = ['PROPOSED', 'COMMITTED', 'IN_PROGRESS', 'DONE', 'CANCELLED'];

function emptyPersonForm() {
  return { name: '', availabilityFte: 1.0, velocity: 5.0 };
}

function PersonForm({ initial, onSubmit, onCancel }) {
  const [form, setForm] = useState(initial ?? emptyPersonForm());
  const update = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      name: form.name,
      availabilityFte: Number(form.availabilityFte),
      velocity: Number(form.velocity),
    });
  };

  return (
    <form className="form form--inline" onSubmit={handleSubmit}>
      <label>
        Name
        <input value={form.name} onChange={update('name')} required />
      </label>
      <label>
        Availability (FTE)
        <input
          type="number"
          min="0"
          max="1"
          step="0.1"
          value={form.availabilityFte}
          onChange={update('availabilityFte')}
          required
        />
      </label>
      <label>
        Velocity (sp/day)
        <input
          type="number"
          min="0"
          step="0.5"
          value={form.velocity}
          onChange={update('velocity')}
          required
        />
      </label>
      <div className="form-actions">
        <button type="submit">Save</button>
        <button type="button" className="button-secondary" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}

function OverheadEditor({ team, onSave }) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(team.overheadPercentage);

  if (!editing) {
    return (
      <span>
        {(team.overheadPercentage * 100).toFixed(0)}%{' '}
        <button className="link-button" onClick={() => setEditing(true)}>
          edit
        </button>
      </span>
    );
  }

  return (
    <span className="inline-edit">
      <input
        type="number"
        min="0"
        max="1"
        step="0.01"
        value={value}
        onChange={(e) => setValue(e.target.value)}
      />
      <button
        onClick={async () => {
          await onSave(Number(value));
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

function emptyEpicForm(teamId) {
  return { name: '', initiativeId: '', teamId, storyPoints: 0, dueDate: '', status: 'PROPOSED' };
}

function EpicForm({ initial, teamId, initiatives, onSubmit, onCancel }) {
  const [form, setForm] = useState(initial ?? emptyEpicForm(teamId));
  const update = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      ...form,
      teamId,
      initiativeId: form.initiativeId === '' ? null : Number(form.initiativeId),
      storyPoints: Number(form.storyPoints),
      dueDate: form.dueDate || null,
    });
  };

  return (
    <form className="form" onSubmit={handleSubmit}>
      <label>
        Name
        <input value={form.name} onChange={update('name')} required />
      </label>
      <label>
        Initiative (optional)
        <select value={form.initiativeId ?? ''} onChange={update('initiativeId')}>
          <option value="">None (standalone)</option>
          {initiatives.map((i) => (
            <option key={i.id} value={i.id}>
              {i.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        Story Points
        <input
          type="number"
          min="0"
          step="1"
          value={form.storyPoints}
          onChange={update('storyPoints')}
          required
        />
      </label>
      <label>
        Due Date
        <input type="date" value={form.dueDate ?? ''} onChange={update('dueDate')} />
      </label>
      <label>
        Status
        <select value={form.status} onChange={update('status')}>
          {EPIC_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </label>
      <div className="form-actions">
        <button type="submit">Save</button>
        <button type="button" className="button-secondary" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
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

  const team = teams?.find((t) => t.id === teamId);
  const teamEpics = (allEpics?.filter((e) => e.teamId === teamId) ?? []).sort(byDueDate);

  const handleOverheadSave = async (overheadPercentage) => {
    await teamsApi.update(teamId, { ...team, overheadPercentage });
    reloadTeams();
    reloadCapacity();
  };

  const handleAddPerson = async (payload) => {
    await teamsApi.addPerson(teamId, payload);
    setAddingPerson(false);
    reloadPeople();
    reloadCapacity();
  };

  const handleUpdatePerson = async (personId, payload) => {
    await teamsApi.updatePerson(teamId, personId, payload);
    setEditingPersonId(null);
    reloadPeople();
    reloadCapacity();
  };

  const handleDeletePerson = async (personId) => {
    if (!confirm('Remove this person from the team?')) return;
    await teamsApi.removePerson(teamId, personId);
    reloadPeople();
    reloadCapacity();
  };

  const handleCreateEpic = async (payload) => {
    await epicsApi.create(payload);
    setAddingEpic(false);
    reloadEpics();
    reloadCapacity();
  };

  const handleUpdateEpic = async (id, payload) => {
    await epicsApi.update(id, { ...payload, id });
    setEditingEpicId(null);
    reloadEpics();
    reloadCapacity();
  };

  const handleDeleteEpic = async (id) => {
    if (!confirm('Delete this epic?')) return;
    await epicsApi.remove(id);
    reloadEpics();
    reloadCapacity();
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
          <PersonForm onSubmit={handleAddPerson} onCancel={() => setAddingPerson(false)} />
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
                      <PersonForm
                        initial={person}
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
          <EpicForm
            teamId={teamId}
            initiatives={initiatives}
            onSubmit={handleCreateEpic}
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
                      <EpicForm
                        initial={epic}
                        teamId={teamId}
                        initiatives={initiatives}
                        onSubmit={(payload) => handleUpdateEpic(epic.id, payload)}
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
