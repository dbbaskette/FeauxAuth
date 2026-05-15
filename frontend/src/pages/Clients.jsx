import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import DataTable from '../components/DataTable'
import { Badge, CopyableMono, ScopeBadge, Button } from '../components/ui'

const columns = [
  { key: 'clientId', label: 'Client ID', sortable: true,
    render: v => <CopyableMono>{v}</CopyableMono> },
  { key: 'name', label: 'Name', sortable: true },
  {
    key: 'allowedScopes',
    label: 'Scopes',
    render: v => (
      <div className="flex flex-wrap gap-1">
        {(v || '').split(/\s+/).filter(Boolean).map(s => <ScopeBadge key={s} scope={s} />)}
      </div>
    ),
  },
  {
    key: 'enabled',
    label: 'Status',
    render: v => v
      ? <Badge variant="success">Enabled</Badge>
      : <Badge variant="danger">Disabled</Badge>,
  },
  { key: 'accessTokenTtl', label: 'Token TTL', sortable: true, render: v => `${v}s` },
]

export default function Clients() {
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/admin/clients').then(c => { setClients(c); setLoading(false) })
  }, [])

  const handleDelete = async (id) => {
    if (!confirm('Delete this client?')) return
    await api.del(`/api/admin/clients/${id}`)
    setClients(clients.filter(c => c.id !== id))
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <div className="eyebrow">Manage</div>
          <h1 className="text-h1 mt-1">OAuth Clients</h1>
        </div>
        <Button as={Link} to="/clients/new" size="sm">+ Register Client</Button>
      </div>

      <DataTable
        columns={columns}
        data={clients}
        loading={loading}
        searchableKeys={['clientId', 'name']}
        defaultSort={{ key: 'name', dir: 'asc' }}
        emptyTitle="No clients yet"
        emptyDescription="Register your first OAuth client to start issuing tokens."
        actions={(row) => (
          <div className="inline-flex items-center gap-3">
            <Link to={`/clients/${row.id}/edit`} className="text-violet-400 hover:text-violet-300 text-sm font-medium">Edit</Link>
            <button onClick={() => handleDelete(row.id)} className="text-rose-400 hover:text-rose-300 text-sm font-medium">Delete</button>
          </div>
        )}
      />
    </div>
  )
}
