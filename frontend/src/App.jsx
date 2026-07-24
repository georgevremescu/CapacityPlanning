import { useState } from 'react';
import Layout from './components/Layout.jsx';
import HigherManagementView from './views/HigherManagementView.jsx';
import EmPmView from './views/EmPmView.jsx';
import TeamLeadView from './views/TeamLeadView.jsx';
import { currentQuarter } from './utils/quarter.js';

export default function App() {
  const [activeTab, setActiveTab] = useState('higher-management');
  const [quarter, setQuarter] = useState(currentQuarter());
  const [mode, setMode] = useState('committed');

  return (
    <Layout
      activeTab={activeTab}
      onChangeTab={setActiveTab}
      quarter={quarter}
      onChangeQuarter={setQuarter}
      mode={mode}
      onChangeMode={setMode}
    >
      {activeTab === 'higher-management' && <HigherManagementView quarter={quarter} />}
      {activeTab === 'em-pm' && <EmPmView quarter={quarter} mode={mode} />}
      {activeTab === 'team-lead' && <TeamLeadView quarter={quarter} mode={mode} />}
    </Layout>
  );
}
