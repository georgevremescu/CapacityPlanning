import { capacityApi } from '../api/index.js';
import { useAsync } from '../utils/useAsync.js';
import { quartersFrom } from '../utils/quarter.js';
import UtilizationBar from '../components/UtilizationBar.jsx';
import { CAPACITY_MODE } from '../constants.js';

const ANNUAL_OUTLOOK_QUARTERS = 4;

function ImpactBadge({ committedSp, simulatedSp }) {
  const delta = simulatedSp - committedSp;
  if (delta <= 0) {
    return <span className="badge badge--committed">No proposed demand</span>;
  }
  return <span className="badge badge--proposed">+{delta.toFixed(0)} sp if proposed work lands</span>;
}

export default function HigherManagementView({ quarter }) {
  const { data, error, loading } = useAsync(async () => {
    const [committed, simulated, annual] = await Promise.all([
      capacityApi.overview(quarter, CAPACITY_MODE.COMMITTED),
      capacityApi.overview(quarter, CAPACITY_MODE.SIMULATED),
      Promise.all(
        quartersFrom(quarter, ANNUAL_OUTLOOK_QUARTERS).map((q) =>
          capacityApi.overview(q, CAPACITY_MODE.COMMITTED),
        ),
      ),
    ]);
    return { committed, simulated, annual };
  }, [quarter]);

  if (loading) return <p className="state-message">Loading overview...</p>;
  if (error) return <p className="state-message state-message--error">{error}</p>;

  const { committed, simulated, annual } = data;

  return (
    <div className="view">
      <div className="view-header">
        <h2>Capacity Outlook</h2>
        <p className="view-subtitle">
          {quarter} &middot; Cross-team capacity vs. the annual business plan
        </p>
      </div>

      <div className="card">
        <h3>Organization Totals</h3>
        <div className="compare-grid">
          <div>
            <p className="compare-label">Committed plan</p>
            <UtilizationBar
              utilization={committed.orgUtilization}
              allocatedSp={committed.orgAllocatedSp}
              capacitySp={committed.orgNetCapacitySp}
            />
          </div>
          <div>
            <p className="compare-label">Including proposed demand</p>
            <UtilizationBar
              utilization={simulated.orgUtilization}
              allocatedSp={simulated.orgAllocatedSp}
              capacitySp={simulated.orgNetCapacitySp}
            />
          </div>
        </div>
        <p className="metric-line">
          <ImpactBadge committedSp={committed.orgAllocatedSp} simulatedSp={simulated.orgAllocatedSp} />
        </p>
      </div>

      <div className="card">
        <h3>Annual Outlook &mdash; Committed Plan</h3>
        <p className="view-subtitle">
          Org-wide committed utilization for the next {ANNUAL_OUTLOOK_QUARTERS} quarters, starting {quarter}
        </p>
        <table className="table">
          <thead>
            <tr>
              <th>Quarter</th>
              <th>Committed Capacity vs. Demand</th>
            </tr>
          </thead>
          <tbody>
            {annual.map((quarterOverview) => (
              <tr key={quarterOverview.quarter}>
                <td>{quarterOverview.quarter}</td>
                <td>
                  <UtilizationBar
                    utilization={quarterOverview.orgUtilization}
                    allocatedSp={quarterOverview.orgAllocatedSp}
                    capacitySp={quarterOverview.orgNetCapacitySp}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
