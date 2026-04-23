import React from 'react';
import { useNavigate } from 'react-router-dom';

const LandingPage = () => {
  const navigate = useNavigate();

  const techStack = [
    { name: 'Spring Boot', icon: 'springboot' },
    { name: 'Apache Kafka', icon: 'apachekafka' },
    { name: 'Gemini AI', icon: 'google' },
    { name: 'PostgreSQL', icon: 'postgresql' },
    { name: 'Redis', icon: 'redis' },
    { name: 'Docker', icon: 'docker' },
    { name: 'Elasticsearch', icon: 'elasticsearch' },
    { name: 'Prometheus', icon: 'prometheus' },
    { name: 'Jira Software', icon: 'jira' }
  ];

  return (
    <div className="wrapper">
      <nav className="nav-container">
        <div className="brand">
          <div className="brand-dot"></div>
          INCIDENT<span>BRAIN</span>
        </div>
        <div style={{ display: 'flex', gap: '2rem', alignItems: 'center' }}>
          <span className="mono" style={{ fontSize: '0.7rem', color: '#22c55e' }}>● SYSTEM_OPERATIONAL</span>
          <button onClick={() => navigate('/login')} className="btn btn-main" style={{ padding: '8px 20px' }}>
            Console
          </button>
        </div>
      </nav>

      <main className="hero-section">
        <div className="hero-tag">
          <span style={{ opacity: 0.6 }}>v1.0.4</span>
          <span style={{ width: '1px', height: '12px', background: '#cbd5e1' }}></span>
          <span>Event-Driven Autopilot for Engineering Teams</span>
        </div>
        
        <h1 className="hero-h1">
          Mean Time to Resolution, <br />
          <span className="gradient-span">Reduced to Seconds.</span>
        </h1>
        
        <p className="hero-p">
          IncidentBrain is a high-throughput microservices ecosystem that captures Prometheus alerts, 
          correlates system context, and uses LLMs to automate your entire Jira ticketing workflow.
        </p>

        <div className="action-group">
          <button onClick={() => navigate('/login')} className="btn btn-main">Deploy Intelligence</button>
          <button className="btn btn-outline">Documentation</button>
        </div>

        <div className="bento-grid">
          <div className="bento-item">
            <div className="mono" style={{ color: '#6366f1', marginBottom: '1rem' }}>[01] INGESTION</div>
            <h3 style={{ marginBottom: '0.8rem' }}>Kafka Streams</h3>
            <p style={{ fontSize: '0.9rem', color: '#64748b' }}>
              Real-time alert pipeline using Apache Kafka to bridge Prometheus thresholds and downstream services.
            </p>
          </div>
          
          <div className="bento-item">
            <div className="mono" style={{ color: '#6366f1', marginBottom: '1rem' }}>[02] ANALYSIS</div>
            <h3 style={{ marginBottom: '0.8rem' }}>LLM Core</h3>
            <p style={{ fontSize: '0.9rem', color: '#64748b' }}>
              Gemini 2.5 Flash processes Elasticsearch logs and Spring Actuator metrics for instant Root Cause Analysis.
            </p>
          </div>

          <div className="bento-item">
            <div className="mono" style={{ color: '#6366f1', marginBottom: '1rem' }}>[03] RESOLUTION</div>
            <h3 style={{ marginBottom: '0.8rem' }}>Auto-Ticketing</h3>
            <p style={{ fontSize: '0.9rem', color: '#64748b' }}>
              Bi-directional Jira integration that creates, assigns, and enriches incident tickets without human input.
            </p>
          </div>
        </div>

        <div style={{ marginTop: '100px', borderTop: '1px solid #e2e8f0', paddingTop: '60px' }}>
          <h4 className="mono" style={{ marginBottom: '30px', opacity: 0.5 }}>INTEGRATED ARCHITECTURE</h4>
          <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: '16px' }}>
            {techStack.map(tech => (
              <div key={tech.name} 
                style={{ 
                  display: 'flex', alignItems: 'center', gap: '10px', 
                  padding: '10px 20px', background: 'white', 
                  borderRadius: '12px', border: '1px solid #f1f5f9',
                  boxShadow: '0 2px 4px rgba(0,0,0,0.02)'
                }}>
                <img 
                  src={`https://cdn.simpleicons.org/${tech.icon}/${tech.icon === 'google' ? '4285F4' : '6366f1'}`} 
                  alt={tech.name} 
                  style={{ width: '18px' }}
                />
                <span style={{ fontSize: '0.85rem', fontWeight: '600' }}>{tech.name}</span>
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
};

export default LandingPage;