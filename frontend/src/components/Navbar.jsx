import { useState, useEffect } from 'react'
import { NavLink } from 'react-router-dom'
import { getUser, isLoggedIn, clearAuth } from '../utils/auth'
import AuthModal from './AuthModal'

function Navbar() {
  const [auth, setAuth] = useState(getUser())
  const [logged, setLogged] = useState(isLoggedIn())
  const [modalOpen, setModalOpen] = useState(false)

  useEffect(() => {
    const handleAuthChange = () => {
      setAuth(getUser())
      setLogged(isLoggedIn())
    }
    window.addEventListener('authChange', handleAuthChange)
    return () => window.removeEventListener('authChange', handleAuthChange)
  }, [])

  return (
    <>
      <nav className="navbar">
        <span className="logo">OmniStock</span>
        <NavLink to="/products" className={({ isActive }) => isActive ? 'active' : ''}>
          Products
        </NavLink>
        <NavLink to="/inventory" className={({ isActive }) => isActive ? 'active' : ''}>
          Inventory
        </NavLink>
        <NavLink to="/orders" className={({ isActive }) => isActive ? 'active' : ''}>
          Orders
        </NavLink>

        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '10px' }}>
          {logged ? (
            <>
              <span style={{ fontSize: '13px', color: '#e0e0e8' }}>
                👤 <strong>{auth.username}</strong>
              </span>
              <button
                type="button"
                className="btn btn-secondary"
                style={{ padding: '4px 10px', fontSize: '12px' }}
                onClick={clearAuth}
              >
                Log Out
              </button>
            </>
          ) : (
            <button
              type="button"
              className="btn btn-primary"
              style={{ padding: '6px 14px', fontSize: '13px' }}
              onClick={() => setModalOpen(true)}
            >
              Log In / Register
            </button>
          )}
        </div>
      </nav>

      <AuthModal isOpen={modalOpen} onClose={() => setModalOpen(false)} />
    </>
  )
}

export default Navbar

