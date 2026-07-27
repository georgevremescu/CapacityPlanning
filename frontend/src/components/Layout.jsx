import { nearbyQuarters } from '../utils/quarter.js';
import { CAPACITY_MODE, TAB } from '../constants.js';

const TABS = [
  {
    id: TAB.HIGHER_MANAGEMENT,
    label: 'Capacity Outlook',
    hint: 'Cross-team capacity & business plan impact',
  },
  {
    id: TAB.EM_PM,
    label: 'Planning Simulation',
    hint: 'Initiative delivery, trade-offs & simulation',
  },
  {
    id: TAB.TEAM_LEAD,
    label: 'Team Workspace',
    hint: 'Roster, overhead & team-level epics',
  },
];

export default function Layout({ activeTab, onChangeTab, quarter, onChangeQuarter, mode, onChangeMode, children }) {
  const quarterOptions = nearbyQuarters();
  // Higher Management always shows committed vs. simulated side by side, so the
  // global toggle (which only ever shows one mode at a time) doesn't apply there.
  const showModeToggle = activeTab !== TAB.HIGHER_MANAGEMENT;

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header-top">
          <h1>Capacity Planning</h1>
          <div className="app-header-controls">
            <label className="control">
              <span>Quarter</span>
              <select value={quarter} onChange={(e) => onChangeQuarter(e.target.value)}>
                {quarterOptions.map((q) => (
                  <option key={q} value={q}>
                    {q}
                  </option>
                ))}
              </select>
            </label>
            {showModeToggle ? (
              <label className="control control--toggle">                
                <input
                  type="checkbox"
                  checked={mode === CAPACITY_MODE.SIMULATED}
                  onChange={(e) =>
                    onChangeMode(e.target.checked ? CAPACITY_MODE.SIMULATED : CAPACITY_MODE.COMMITTED)
                  }
                />
                <span className="control-hint">
                  {mode === CAPACITY_MODE.SIMULATED ? 'Including proposed' : 'Committed only'}
                </span>
              </label>
            ) : (
              <span className="control-hint">Showing committed vs. proposed side by side</span>
            )}
          </div>
        </div>
        <nav className="app-tabs">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              className={`app-tab ${activeTab === tab.id ? 'app-tab--active' : ''}`}
              onClick={() => onChangeTab(tab.id)}
              title={tab.hint}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </header>
      <main className="app-main">{children}</main>
    </div>
  );
}
