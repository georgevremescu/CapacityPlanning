import { nearbyQuarters } from '../utils/quarter.js';

const TABS = [
  {
    id: 'higher-management',
    label: 'Capacity Outlook',
    hint: 'Cross-team capacity & business plan impact',
  },
  {
    id: 'em-pm',
    label: 'Planning Simulation',
    hint: 'Initiative delivery, trade-offs & simulation',
  },
  {
    id: 'team-lead',
    label: 'Team Workspace',
    hint: 'Roster, overhead & team-level epics',
  },
];

export default function Layout({ activeTab, onChangeTab, quarter, onChangeQuarter, mode, onChangeMode, children }) {
  const quarterOptions = nearbyQuarters();
  // Higher Management always shows committed vs. simulated side by side, so the
  // global toggle (which only ever shows one mode at a time) doesn't apply there.
  const showModeToggle = activeTab !== 'higher-management';

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
                <span>Simulation</span>
                <input
                  type="checkbox"
                  checked={mode === 'simulated'}
                  onChange={(e) => onChangeMode(e.target.checked ? 'simulated' : 'committed')}
                />
                <span className="control-hint">
                  {mode === 'simulated' ? 'Including proposed' : 'Committed only'}
                </span>
              </label>
            ) : (
              <span className="control-hint">Showing committed vs. simulated side by side</span>
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
