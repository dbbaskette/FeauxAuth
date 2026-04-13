import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import DataTable from '../components/DataTable'

const columns = [
  { key: 'clientId', label: 'Client ID' },
  { key: 'name', label: 'Name' },
  { key: 'allowedScopes', label: 'Scopes' },
  { key: 'enabled', label: 'Enabled', render: v => v ? 'Yes' : 'No' },
  { key: 'accessTokenTtl', label: 'Token TTL', render: v => `${v}s` },
]

export default function Clients() {
  const [clients, setClients] = useState([])

  useEffect(() => {
    api.get('/api/admin/clients').then(setClients)
  }, [])

  const handleDelete = async (id) => {
    if (!confirm('Delete this client?')) return
    await api.del(`/api/admin/clients/${id}`)
    setClients(clients.filter(c => c.id !== id))
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-white">OAuth Clients</h1>
        <Link
          to="/clients/new"
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition-colors"
        >
          Add Client
        </Link>
      </div>

      <DataTable
        columns={columns}
        data={clients}
        actions={(row) => (
          <div className="space-x-3">
            <Link to={`/clients/${row.id}/edit`} className="text-indigo-400 hover:text-indigo-300">Edit</Link>
            <button onClick={() => handleDelete(row.id)} className="text-red-400 hover:text-red-300">Delete</button>
          </div>
        )}
      />
    </div>
  )
}
