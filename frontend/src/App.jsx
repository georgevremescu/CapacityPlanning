import { useState } from 'react';
import Layout from './components/Layout.jsx';
import HigherManagementView from './views/HigherManagementView.jsx';
import EmPmView from './views/EmPmView.jsx';
import TeamLeadView from './views/TeamLeadView.jsx';
import { currentQuarter } from './utils/quarter.js';
import { CAPACITY_MODE, TAB } from './constants.js';

export default function App() {
  const [activeTab, setActiveTab] = useState(TAB.HIGHER_MANAGEMENT);
  const [quarter, setQuarter] = useState(currentQuarter());
  const [mode, setMode] = useState(CAPACITY_MODE.COMMITTED);

  return (
    <Layout
      activeTab={activeTab}
      onChangeTab={setActiveTab}
      quarter={quarter}
      onChangeQuarter={setQuarter}
      mode={mode}
      onChangeMode={setMode}
    >
      {activeTab === TAB.HIGHER_MANAGEMENT && <HigherManagementView quarter={quarter} />}
      {activeTab === TAB.EM_PM && <EmPmView quarter={quarter} mode={mode} />}
      {activeTab === TAB.TEAM_LEAD && <TeamLeadView quarter={quarter} mode={mode} />}
    </Layout>
  );
}
