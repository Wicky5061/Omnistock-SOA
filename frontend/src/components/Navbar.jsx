import { NavLink } from 'react-router-dom'

function Navbar() {
  return (
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
    </nav>
  )
}

export default Navbar
