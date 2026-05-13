import { useState, useEffect } from 'react'
import { api } from '../api/client'
import StatCard from '../components/StatCard'
import DataTable from '../components/DataTable'
import { tokenColumns } from '../components/tokenColumns'
import { Badge, Mono, SkeletonText } from '../components/ui'

const dashboardColumns = [
  ...tokenColumns,
  {
    key: 'revoked',
    label: 'Status',
    render: v => v
      ? <Badge variant="danger">Revoked</Badge>
      : <Badge variant="success">Active</Badge>,
  },
]

const ICON = {
  clients: (
    <svg width="14" height="14" viewBox="0 0 20 20" fill="currentColor"><path d="M10 2a4 4 0 014 4v2h1a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2h1V6a4 4 0 014-4zm2 6V6a2 2 0 10-4 0v2h4z"/></svg>
  ),
  users: (
    <svg width="14" height="14" viewBox="0 0 20 20" fill="currentColor"><path d="M10 10a4 4 0 100-8 4 4 0 000 8zm0 2c-4 0-8 2-8 6v2h16v-2c0-4-4-6-8-6z"/></svg>
  ),
  tokens: (
    <svg width="14" height="14" viewBox="0 0 20 20" fill="currentColor"><path d="M11 3a1 1 0 011 1v5h5a1 1 0 010 2h-5v5a1 1 0 11-2 0v-5H5a1 1 0 010-2h5V4a1 1 0 011-1z"/></svg>
  ),
  key: (
    <svg width="14" height="14" viewBox="0 0 20 20" fill="currentColor"><path d="M10 2l7 4v6c0 3.5-3 6.5-7 8-4-1.5-7-4.5-7-8V6l7-4z"/></svg>
  ),
}

export default function Dashboard() {
  const [stats, setStats] = useState(null)

  useEffect(() => {
    api.get('/api/admin/dashboard/stats').then(setStats)
  }, [])

  return (
    <div>
      <div className="mb-7">
        <div className="eyebrow">Overview</div>
        <h1 className="text-h1 mt-1">Dashboard</h1>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {stats ? (
          <>
            <StatCard label="OAuth Clients" value={stats.totalClients} icon={ICON.clients} />
            <StatCard label="Users" value={stats.totalUsers} icon={ICON.users} />
            <StatCard label="Active Tokens" value={stats.activeTokens} icon={ICON.tokens} />
            <StatCard
              label="Signing Key"
              value={<Mono className="text-[18px]">{stats.signingKeyId}</Mono>}
              icon={ICON.key}
            />
          </>
        ) : (
          Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="card">
              <SkeletonText lines={2} />
            </div>
          ))
        )}
      </div>

      <h2 className="text-h2 mb-3">Recent Tokens</h2>
      <DataTable
        columns={dashboardColumns}
        data={stats?.recentTokens || []}
        emptyTitle="No tokens yet"
        emptyDescription="Tokens issued through the OAuth flows will appear here."
      />
    </div>
  )
}
