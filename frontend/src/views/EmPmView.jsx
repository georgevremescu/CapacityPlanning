import { useState } from 'react';
import { capacityApi, initiativesApi } from '../api/index.js';
import { useAsync } from '../utils/useAsync.js';
import UtilizationBar from '../components/UtilizationBar.jsx';
import CrudForm from '../components/CrudForm.jsx';

const STATUSES = ['PROPOSED', 'COMMITTED', 'CANCELLED'];

const EMPTY_FORM = {
  name: '',
  description: '',
  estimatedStoryPoints: 0,
  targetDate: '',
  status: 'PROPOSED',
  priority: '',
};

const INITIATIVE_FIELDS = [
  { name: 'name', label: 'Name', type: 'text', required: true },
  { name: 'description', label: 'Description', type: 'textarea', rows: 2 },
  {
    name: 'estimatedStoryPoints',
    label: 'Estimated Story Points',
    type: 'number',
    min: 0,
    step: 1,
    required: true,
  },
  { name: 'targetDate', label: 'Target Date', type: 'date' },
  {
    name: 'status',
    label: 'Status',
    type: 'select',
    options: STATUSES.map((s) => ({ value: s, label: s })),
  },
  { name: 'priority', label: 'Priority (for trade-off ranking)', type: 'number', nullable: true },
];

function InitiativeDetail({ initiativeId, quarter, mode, onClose }) {
  const { data, error, loading } = useAsync(async () => {
    const detail = await initiativesApi.get(initiativeId);
    const teamCapacities = await Promise.all(
      detail.teamsInvolved.map((team) => capacityApi.forTeam(team.id, quarter, mode)),
    );
    return { detail, teamCapacities };
  }, [initiativeId, quarter, mode]);

  return (
    <div className="card">
      <div className="card-header">
        <h3>Initiative Detail</h3>
        <button className="button-secondary" onClick={onClose}>
          Close
        </button>
      </div>
      {loading && <p className="state-message">Loading...</p>}
      {error && <p className="state-message state-message--error">{error}</p>}
      {data && (
        <>
          <p>
            Rolled-up epic story points: <strong>{data.detail.rolledUpEpicStoryPoints}</strong>
          </p>

          <h4>Delivery across teams &mdash; {quarter}</h4>
          <p className="view-subtitle">
            {mode === 'simulated' ? 'Including proposed work' : 'Committed only'} &mdash; capacity
            each involved team has left after this quarter's other assigned work
          </p>
          {data.teamCapacities.length === 0 ? (
            <p className="state-message">No teams involved yet &mdash; add an epic below.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Team</th>
                  <th>Capacity vs. Demand</th>
                </tr>
              </thead>
              <tbody>
                {data.teamCapacities.map((tc) => (
                  <tr key={tc.teamId}>
                    <td>{tc.teamName}</td>
                    <td>
                      <UtilizationBar
                        utilization={tc.utilization}
                        allocatedSp={tc.allocatedSp}
                        capacitySp={tc.netCapacitySp}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <h4>Epics</h4>
          {data.detail.epics.length === 0 ? (
            <p className="state-message">No epics assigned yet.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Team</th>
                  <th>Story Points</th>
                  <th>Due Date</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {data.detail.epics.map((epic) => (
                  <tr key={epic.id}>
                    <td>{epic.name}</td>
                    <td>{epic.teamName}</td>
                    <td>{epic.storyPoints}</td>
                    <td>{epic.dueDate ?? '-'}</td>
                    <td>{epic.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  );
}

// Nulls sort last: initiatives without a priority don't crowd out ranked trade-offs.
function byPriority(a, b) {
  if (a.priority == null && b.priority == null) return 0;
  if (a.priority == null) return 1;
  if (b.priority == null) return -1;
  return a.priority - b.priority;
}

export default function EmPmView({ quarter, mode }) {
  const { data: initiatives, error, loading, reload } = useAsync(
    () => initiativesApi.list(),
    [],
  );
  const [editingId, setEditingId] = useState(null);
  const [creating, setCreating] = useState(false);
  const [detailId, setDetailId] = useState(null);
  const [actionError, setActionError] = useState(null);

  const handleCreate = async (payload) => {
    try {
      await initiativesApi.create(payload);
      setActionError(null);
      setCreating(false);
      reload();
    } catch (err) {
      setActionError(err.message);
    }
  };

  const handleUpdate = async (id, payload) => {
    try {
      await initiativesApi.update(id, { ...payload, id });
      setActionError(null);
      setEditingId(null);
      reload();
    } catch (err) {
      setActionError(err.message);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this initiative? Its epics will remain, unlinked.')) return;
    try {
      await initiativesApi.remove(id);
      setActionError(null);
      reload();
    } catch (err) {
      setActionError(err.message);
    }
  };

  if (loading) return <p className="state-message">Loading initiatives...</p>;
  if (error) return <p className="state-message state-message--error">{error}</p>;

  const sortedInitiatives = [...initiatives].sort(byPriority);

  return (
    <div className="view">
      <div className="view-header">
        <h2>Planning Simulation</h2>
        <button onClick={() => setCreating((v) => !v)}>
          {creating ? 'Cancel' : 'New Initiative'}
        </button>
      </div>
      <p className="view-subtitle">
        Ranked by priority for trade-off decisions. Simulation mode ({mode === 'simulated' ? 'including proposed' : 'committed only'}) drives the per-team capacity shown in each initiative's detail.
      </p>

      {actionError && <p className="state-message state-message--error">{actionError}</p>}

      {creating && (
        <div className="card">
          <CrudForm
            fields={INITIATIVE_FIELDS}
            initialValues={EMPTY_FORM}
            onSubmit={handleCreate}
            onCancel={() => setCreating(false)}
          />
        </div>
      )}

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Priority</th>
              <th>Name</th>
              <th>Status</th>
              <th>Est. SP</th>
              <th>Target Date</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {sortedInitiatives.map((initiative) =>
              editingId === initiative.id ? (
                <tr key={initiative.id}>
                  <td colSpan={6}>
                    <CrudForm
                      fields={INITIATIVE_FIELDS}
                      initialValues={initiative}
                      onSubmit={(payload) => handleUpdate(initiative.id, payload)}
                      onCancel={() => setEditingId(null)}
                    />
                  </td>
                </tr>
              ) : (
                <tr key={initiative.id}>
                  <td>{initiative.priority ?? '-'}</td>
                  <td>
                    <button className="link-button" onClick={() => setDetailId(initiative.id)}>
                      {initiative.name}
                    </button>
                  </td>
                  <td>
                    <span className={`badge badge--${initiative.status.toLowerCase()}`}>
                      {initiative.status}
                    </span>
                  </td>
                  <td>{initiative.estimatedStoryPoints}</td>
                  <td>{initiative.targetDate ?? '-'}</td>
                  <td className="table-actions">
                    <button className="button-secondary" onClick={() => setEditingId(initiative.id)}>
                      Edit
                    </button>
                    <button className="button-danger" onClick={() => handleDelete(initiative.id)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ),
            )}
          </tbody>
        </table>
      </div>

      {detailId && (
        <InitiativeDetail
          initiativeId={detailId}
          quarter={quarter}
          mode={mode}
          onClose={() => setDetailId(null)}
        />
      )}
    </div>
  );
}
