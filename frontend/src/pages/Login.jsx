import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { Button, Input } from '../components/ui'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    api.login(username, password)

    try {
      const res = await api.fetch('/api/admin/dashboard/stats')
      if (res.ok) {
        navigate('/')
      } else {
        api.logout()
        setError('Invalid credentials')
      }
    } catch {
      setError('Invalid credentials')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="card-elev">
          <div className="text-center mb-7">
            <div className="brand-mark mx-auto mb-3 w-11 h-11 text-[16px]">F</div>
            <div className="eyebrow">Admin Console</div>
            <h1 className="text-2xl font-bold text-text mt-1">FeauxAuth</h1>
            <p className="text-text-dim text-sm mt-1">Sign in to manage clients, users, and tokens.</p>
          </div>

          {error && (
            <div className="rounded border border-rose-500/30 bg-rose-500/5 text-rose-300 px-3.5 py-2.5 text-sm mb-5">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              id="username"
              label="Username"
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
              placeholder="admin"
              autoComplete="username"
            />
            <Input
              id="password"
              label="Password"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />
            <Button type="submit" block disabled={loading}>
              {loading ? 'Signing in…' : 'Sign In'}
            </Button>
          </form>
        </div>
        <p className="text-center text-xs text-text-mute mt-5">FeauxAuth — Lab / Demo use only. Not for production.</p>
      </div>
    </div>
  )
}
