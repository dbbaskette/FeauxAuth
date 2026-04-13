import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { api } from '../api/client'

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
    <div className="min-h-screen bg-gray-900">
      <nav className="bg-gray-800 border-b border-gray-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-8">
              <span className="text-xl font-bold text-white">FeauxAuth</span>
              <div className="flex space-x-1">
                {navItems.map(item => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      `px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                        isActive
                          ? 'bg-gray-900 text-white'
                          : 'text-gray-300 hover:bg-gray-700 hover:text-white'
                      }`
                    }
                  >
                    {item.label}
                  </NavLink>
                ))}
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="text-gray-300 hover:text-white text-sm font-medium transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}
