import { useState } from 'react'
import { setAuth } from '../utils/auth'

function AuthModal({ isOpen, onClose }) {
  const [isRegister, setIsRegister] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (!isOpen) return null

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)

    const endpoint = isRegister ? '/auth/register' : '/auth/login'
    try {
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })
      const data = await res.json()
      if (!res.ok) {
        throw new Error(data.error || 'Authentication failed')
      }

      setAuth(data.token, data.username, data.role)
      setUsername('')
      setPassword('')
      onClose()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={overlayStyle} onClick={onClose}>
      <div style={modalStyle} onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h2 style={{ margin: 0, fontSize: '20px' }}>{isRegister ? 'Register Account' : 'Log In'}</h2>
          <button onClick={onClose} style={closeButtonStyle}>×</button>
        </div>

        {error && <div className="error" style={{ marginBottom: '16px' }}>⚠ {error}</div>}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '14px' }}>
            <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: 600 }}>Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter username"
              required
              style={inputStyle}
            />
          </div>

          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: 600 }}>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter password"
              required
              style={inputStyle}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', padding: '10px', fontSize: '15px' }}
            disabled={submitting}
          >
            {submitting ? 'Processing...' : (isRegister ? 'Create Account' : 'Log In')}
          </button>
        </form>

        <div style={{ marginTop: '16px', textAlign: 'center', fontSize: '13px' }}>
          {isRegister ? (
            <span>Already have an account? <button type="button" onClick={() => { setIsRegister(false); setError(null); }} style={linkButtonStyle}>Log In</button></span>
          ) : (
            <span>Need an account? <button type="button" onClick={() => { setIsRegister(true); setError(null); }} style={linkButtonStyle}>Sign Up</button></span>
          )}
        </div>
      </div>
    </div>
  )
}

const overlayStyle = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: 'rgba(0, 0, 0, 0.5)',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  zIndex: 1000,
}

const modalStyle = {
  backgroundColor: '#fff',
  borderRadius: '10px',
  padding: '24px',
  width: '100%',
  maxWidth: '380px',
  boxShadow: '0 4px 20px rgba(0, 0, 0, 0.15)',
}

const closeButtonStyle = {
  background: 'none',
  border: 'none',
  fontSize: '24px',
  cursor: 'pointer',
  color: '#888',
}

const inputStyle = {
  width: '100%',
  padding: '9px 12px',
  borderRadius: '6px',
  border: '1px solid #ccc',
  fontSize: '14px',
  boxSizing: 'border-box',
}

const linkButtonStyle = {
  background: 'none',
  border: 'none',
  color: '#4f46e5',
  cursor: 'pointer',
  fontWeight: 600,
  textDecoration: 'underline',
  padding: 0,
}

export default AuthModal
