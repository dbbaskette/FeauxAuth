import { useState, useEffect } from 'react'
import { api } from '../api/client'
import StatCard from '../components/StatCard'
import DataTable from '../components/DataTable'

const tokenColumns = [
  { key: 'jti', label: 'Token ID', render: v => v?.substring(0, 8) + '...' },
  { key: 'clientId', label: 'Client' },
  { key: 'userId', label: 'User', render: v => v?.substring(0, 8) + '...' },
  { key: 'scope', label: 'Scopes' },
  { key: 'createdAt', label: 'Issued', render: v => new Date(v).toLocaleString() },
  { key: 'expiresAt', label: 'Expires', render: v => new Date(v).toLocaleString() },
  { key: 'revoked', label: 'Revoked', render: v => v ? 'Yes' : 'No' },
]

export default function Dashboard() {
  const [stats, setStats] = useState(null)

  useEffect(() => {
    api.get('/api/admin/dashboard/stats').then(setStats)
  }, [])

  if (!stats) return <div className="text-gray-400">Loading...</div>

  return (
    <div>
      <h1 className="text-2xl font-bold text-white mb-6">Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <StatCard label="Total Clients" value={stats.totalClients} />
        <StatCard label="Total Users" value={stats.totalUsers} />
        <StatCard label="Active Tokens" value={stats.activeTokens} />
        <StatCard label="Signing Key" value={stats.signingKeyId} />
      </div>

      <h2 className="text-lg font-semibold text-white mb-4">Recent Tokens</h2>
      <DataTable columns={tokenColumns} data={stats.recentTokens || []} />
    </div>
  )
}
