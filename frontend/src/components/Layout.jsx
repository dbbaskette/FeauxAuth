import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { Button } from './ui'

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/clients', label: 'OAuth Clients' },
  { to: '/users', label: 'Users' },
  { to: '/tokens', label: 'Active Tokens' },
  { to: '/inspector', label: 'Token Inspector' },
]

export default function Layout() {
  const navigate = useNavigate()

  const handleLogout = () => {
    api.logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen">
      <nav className="border-b border-border bg-surface-1/80 backdrop-blur-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-8">
              <div className="flex items-center gap-2.5">
                <span className="brand-mark">F</span>
                <span className="font-semibold text-text tracking-tight">FeauxAuth</span>
              </div>
              <div className="flex gap-1">
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
            </div>
            <Button variant="ghost" size="sm" onClick={handleLogout}>Logout</Button>
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}
