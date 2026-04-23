import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Activity, Shield, AlertCircle,
  Ticket, Loader2
} from 'lucide-react';

const API_BASE = "http://localhost:8080";

const Dashboard = () => {
  const [incidents, setIncidents] = useState([]);
  const [monitors, setMonitors] = useState({});
  const [jiraStats, setJiraStats] = useState({});
  const [activeModal, setActiveModal] = useState(null);
  const [analysisData, setAnalysisData] = useState(null);

  const [loading, setLoading] = useState(false);
  const [pageLoading, setPageLoading] = useState(true);

  const token = localStorage.getItem('token');

  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };

  // ================= FETCH =================
  const fetchData = async () => {
    try {
      const [incRes, monRes, jiraRes] = await Promise.all([
        fetch(`${API_BASE}/context/dashboard/active`, { headers }),
        fetch(`${API_BASE}/ingest/status`, { headers }),
        fetch(`${API_BASE}/api/jira/credentials/metrics`, { headers })
      ]);

      if (incRes.ok) setIncidents(await incRes.json());
      if (monRes.ok) setMonitors(await monRes.json());
      if (jiraRes.ok) setJiraStats(await jiraRes.json());

    } catch (err) {
      console.error(err);
    } finally {
      setPageLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 10000);
    return () => clearInterval(interval);
  }, []);

  // ================= CONFIG =================
  const handleConfigSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    const form = new FormData(e.target);

    try {
      const res = await fetch(`${API_BASE}/api/config/endpoints`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          esUrl: form.get('esUrl'),
          metricsTemplate: form.get('metricsTemplate')
        })
      });

      const text = await res.text();
      if (!res.ok) throw new Error(text);

      alert(text);
      setActiveModal(null);

    } catch (err) {
      alert(err.message);
    }

    setLoading(false);
  };

  // ================= MONITOR =================
  const handleMonitorSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    const form = new FormData(e.target);

    try {
      const res = await fetch(
        `${API_BASE}/ingest/subscribe?url=${encodeURIComponent(form.get('url'))}&name=${encodeURIComponent(form.get('name'))}&threshold=${encodeURIComponent(form.get('threshold'))}`,
        { method: 'POST', headers }
      );

      const text = await res.text();
      if (!res.ok) throw new Error(text);

      alert(text);
      setActiveModal(null);
      fetchData();

    } catch (err) {
      alert(err.message);
    }

    setLoading(false);
  };

  // ================= JIRA =================
  const handleJiraSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    const form = new FormData(e.target);

    try {
      const res = await fetch(`${API_BASE}/api/jira/credentials`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          name: form.get('name'),
          baseUrl: form.get('baseUrl'),
          userEmail: form.get('userEmail'),
          apiToken: form.get('apiToken'),
          projectKey: form.get('projectKey'),
          active: true
        })
      });

      if (!res.ok) throw new Error("Failed to save Jira");

      alert("Jira Saved");
      setActiveModal(null);
      fetchData();

    } catch (err) {
      alert(err.message);
    }

    setLoading(false);
  };

  // ================= ANALYSIS =================
  const runAnalysis = async (id) => {
    try {
      const res = await fetch(`${API_BASE}/context/dashboard/analysis/${id}`, { headers });
      if (!res.ok) throw new Error("Analysis failed");

      setAnalysisData(await res.json());

    } catch (err) {
      alert(err.message);
    }
  };

  // ================= LOADING =================
  if (pageLoading) {
    return (
      <div className="auth-wrapper">
        <Loader2 className="animate-spin" size={40} />
      </div>
    );
  }

  return (
    <div className="dashboard">

      {/* HEADER */}
      <div className="dashboard-header">
        <div>
          <h1 className="dashboard-title">IB.DASHBOARD</h1>
          <p className="sub-text">Incident Control System</p>
        </div>

        <div className="action-group">
          <button className="btn btn-outline" onClick={() => setActiveModal('config')}>CONFIG</button>
          <button className="btn btn-outline" onClick={() => setActiveModal('jira')}>JIRA</button>
          <button className="btn btn-solid" onClick={() => setActiveModal('monitor')}>MONITOR</button>
        </div>
      </div>

      {/* STATS */}
      <div className="stats-grid">
        <StatCard title="Incidents" value={incidents.length} icon={<AlertCircle size={18} />} />
        <StatCard title="Tickets" value={jiraStats.totalTicketsCreated || 0} icon={<Ticket size={18} />} />
        <StatCard title="Monitors" value={Object.keys(monitors).length} icon={<Activity size={18} />} />
        <StatCard title="Auth" value={jiraStats.activeCredentialSets || 0} icon={<Shield size={18} />} />
      </div>

      {/* INCIDENTS */}
      <div className="incidents">
        <h3>Incidents</h3>

        {incidents.length === 0 ? (
          <p className="sub-text">No incidents</p>
        ) : (
          incidents.map(i => (
            <div key={i.incidentId} className="incident-card">
              <div>
                <b>{i.service}</b>
                <span className="mono"> | {i.severity} | {i.status}</span>
              </div>

              <button className="btn btn-solid" onClick={() => runAnalysis(i.incidentId)}>
                ANALYZE
              </button>
            </div>
          ))
        )}
      </div>

      {/* MODALS */}
      <AnimatePresence>

        {activeModal && (
          <div className="modal-overlay">
            <motion.div className="modal" initial={{ scale: 0.9 }} animate={{ scale: 1 }}>

              <button className="modal-close" onClick={() => setActiveModal(null)}>X</button>

              {activeModal === 'config' && (
                <form onSubmit={handleConfigSubmit}>
                  <input className="form-input" name="esUrl" placeholder="ES URL" required />
                  <input className="form-input" name="metricsTemplate" placeholder="Metrics URL" required />

                  <button className="btn-solid" disabled={loading}>
                    {loading ? <span className="spinner"></span> : "SAVE"}
                  </button>
                </form>
              )}

              {activeModal === 'monitor' && (
                <form onSubmit={handleMonitorSubmit}>
                  <input className="form-input" name="url" placeholder="Prometheus URL" required />
                  <input className="form-input" name="name" placeholder="Service Name" required />
                  <input className="form-input" name="threshold" type="number" step="0.001" min="0" placeholder="Latency Threshold" required />

                  <button className="btn-solid" disabled={loading}>
                    {loading ? <span className="spinner"></span> : "START"}
                  </button>
                </form>
              )}

              {activeModal === 'jira' && (
                <form onSubmit={handleJiraSubmit}>
                  <input className="form-input" name="name" placeholder="Name" required />
                  <input className="form-input" name="baseUrl" placeholder="Base URL" required />
                  <input className="form-input" name="userEmail" placeholder="Email" required />
                  <input className="form-input" name="apiToken" placeholder="Token" required />
                  <input className="form-input" name="projectKey" placeholder="Project Key" required />

                  <button className="btn-solid" disabled={loading}>
                    {loading ? <span className="spinner"></span> : "SAVE"}
                  </button>
                </form>
              )}

            </motion.div>
          </div>
        )}

        {analysisData && (
          <div className="modal-overlay">
            <motion.div className="modal">

              <button className="modal-close" onClick={() => setAnalysisData(null)}>X</button>

              <h3>Analysis</h3>
              <p><b>Root Cause:</b> {analysisData.rootCause}</p>
              <p><b>Impact:</b> {analysisData.impactScore}%</p>
              <p><b>Confidence:</b> {analysisData.confidenceLevel}</p>
              <p><b>Fix:</b> {analysisData.suggestedFix}</p>

            </motion.div>
          </div>
        )}

      </AnimatePresence>

    </div>
  );
};

// ================= STAT CARD =================
const StatCard = ({ title, value, icon }) => (
  <div className="stat-card">
    <div className="stat-icon">{icon}</div>
    <h2>{value}</h2>
    <p className="sub-text">{title}</p>
  </div>
);

export default Dashboard;