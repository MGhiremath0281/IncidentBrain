import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';

const Register = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [status, setStatus] = useState('idle'); // idle, loading, success
  const navigate = useNavigate();

  const handleRegister = async (e) => {
    e.preventDefault();
    setStatus('loading');
    try {
      const response = await fetch('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        setStatus('success');
        setTimeout(() => navigate('/login'), 2200);
      } else {
        setStatus('idle');
        alert('Provisioning Failed: Check database constraints.');
      }
    } catch (error) {
      setStatus('idle');
      console.error('Registration error:', error);
    }
  };

  return (
    <div className="auth-wrapper">
      <motion.div 
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className="auth-card"
        style={{ borderTop: '4px solid var(--primary)' }}
      >
        <AnimatePresence mode="wait">
          {status !== 'success' ? (
            <motion.div key="form" exit={{ opacity: 0, y: -20 }}>
              <div className="auth-header">
                <div className="mono-tag">IB.PROVISIONING_NODE</div>
                <h2>Register Operator</h2>
              </div>
              <form onSubmit={handleRegister}>
                <motion.div initial={{ x: -10, opacity: 0 }} animate={{ x: 0, opacity: 1 }} transition={{ delay: 0.1 }} className="form-group">
                  <label className="form-label">Username</label>
                  <input className="form-input" value={username} onChange={(e) => setUsername(e.target.value)} required />
                </motion.div>
                <motion.div initial={{ x: -10, opacity: 0 }} animate={{ x: 0, opacity: 1 }} transition={{ delay: 0.2 }} className="form-group">
                  <label className="form-label">Password</label>
                  <input className="form-input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                </motion.div>
                <button type="submit" disabled={status === 'loading'} className="btn-solid" style={{ width: '100%' }}>
                  {status === 'loading' ? (
                    <><span className="spinner"></span> INITIALIZING...</>
                  ) : 'REGISTER NODE'}
                </button>
              </form>
            </motion.div>
          ) : (
            <motion.div 
              key="success" 
              initial={{ opacity: 0, scale: 0.8 }} 
              animate={{ opacity: 1, scale: 1 }}
              style={{ textAlign: 'center', padding: '1rem' }}
            >
              <motion.div 
                animate={{ rotate: [0, 10, -10, 0] }} 
                transition={{ repeat: Infinity, duration: 2 }}
                style={{ fontSize: '4rem', marginBottom: '1rem' }}
              >
                📡
              </motion.div>
              <h3 className="purple-text">NODE ONLINE</h3>
              <p className="sub-text">Handshaking with Auth-Service...</p>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    </div>
  );
};

export default Register;