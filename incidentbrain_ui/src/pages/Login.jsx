import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const response = await fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        const data = await response.json();
        localStorage.setItem('token', data.token);
        setTimeout(() => navigate('/dashboard'), 1000);
      } else {
        alert('Access Denied: Invalid Operator Credentials');
      }
    } catch (error) {
      console.error('Login error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <motion.div 
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ type: 'spring', damping: 12 }}
        className="auth-card"
      >
        <div className="auth-header">
          <div className="mono-tag">IB.SECURE_GATEWAY</div>
          <h2>System Access</h2>
        </div>

        <form onSubmit={handleLogin}>
          <div className="form-group">
            <label className="form-label">Username</label>
            <input className="form-input" value={username} onChange={(e) => setUsername(e.target.value)} required />
          </div>
          <div className="form-group">
            <label className="form-label">Password</label>
            <input className="form-input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          <button type="submit" disabled={isLoading} className="btn-solid" style={{ width: '100%' }}>
            {isLoading ? (
              <><span className="spinner"></span> VERIFYING...</>
            ) : 'AUTHORIZE ACCESS'}
          </button>
        </form>

        <div className="auth-footer">
          New Node? <span className="auth-link" onClick={() => navigate('/register')}>Request Provisioning</span>
        </div>
      </motion.div>
    </div>
  );
};

export default Login;