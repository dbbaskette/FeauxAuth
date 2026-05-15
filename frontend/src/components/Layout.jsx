import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { Button } from './ui'

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/clients', label: 'OAuth Clients' },
  { to: '/users', label: 'Users' },
  { to: '/tokens', label: 'Active Tokens' },
  { to: '/inspector', label: 'Token Inspector' },
]

const HamburgerIcon = ({ open }) => (
  <svg width="18" height="18" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
    {open
      ? <path d="M4.3 4.3a1 1 0 011.4 0L10 8.6l4.3-4.3a1 1 0 111.4 1.4L11.4 10l4.3 4.3a1 1 0 11-1.4 1.4L10 11.4l-4.3 4.3a1 1 0 11-1.4-1.4L8.6 10 4.3 5.7a1 1 0 010-1.4z"/>
      : <path d="M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z"/>}
  </svg>
)

export default function Layout() {
  const navigate = useNavigate()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)

  // Close the mobile menu on route change.
  useEffect(() => { setMenuOpen(false) }, [location.pathname])

  const handleLogout = () => {
    api.logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen">
      <nav className="border-b border-border bg-surface-1/80 backdrop-blur-sm">
        <div className="px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16 gap-3">
            <div className="flex items-center gap-2.5 min-w-0">
              <span className="brand-mark shrink-0">F</span>
              <span className="font-semibold text-text tracking-tight truncate">FeauxAuth</span>
            </div>

            {/* Desktop nav links — hidden below md */}
            <div className="hidden md:flex items-center gap-1 ml-4 mr-auto">
              {navItems.map(item => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`}
                >
                  {item.label}
                </NavLink>
              ))}
            </div>

            {/* Desktop logout — hidden below md */}
            <div className="hidden md:block shrink-0">
              <Button variant="ghost" size="sm" onClick={handleLogout}>Logout</Button>
            </div>

            {/* Mobile menu toggle — visible below md */}
            <button
              type="button"
              onClick={() => setMenuOpen(o => !o)}
              className="md:hidden inline-grid place-items-center w-9 h-9 rounded
                         text-text-dim hover:text-text hover:bg-surface-2 transition-colors
                         shrink-0"
              aria-label={menuOpen ? 'Close menu' : 'Open menu'}
              aria-expanded={menuOpen}
            >
              <HamburgerIcon open={menuOpen} />
            </button>
          </div>

          {/* Mobile menu panel — only shown when toggled, only below md */}
          {menuOpen && (
            <div className="md:hidden pb-3 pt-1 flex flex-col gap-1 border-t border-border">
              {navItems.map(item => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) => `nav-link${isActive ? ' is-active' : ''}`}
                >
                  {item.label}
                </NavLink>
              ))}
              <div className="pt-2 mt-1 border-t border-border">
                <button
                  type="button"
                  onClick={handleLogout}
                  className="nav-link text-rose-400 hover:!text-rose-300 w-full text-left"
                >
                  Logout
                </button>
              </div>
            </div>
          )}
        </div>
      </nav>

      <main className="px-4 sm:px-6 lg:px-8 py-8 min-w-0">
        <Outlet />
      </main>
    </div>
  )
}
