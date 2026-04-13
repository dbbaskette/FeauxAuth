import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import DataTable from '../components/DataTable'

const columns = [
  { key: 'email', label: 'Email' },
  { key: 'displayName', label: 'Display Name' },
  { key: 'enabled', label: 'Enabled', render: v => v ? 'Yes' : 'No' },
  { key: 'lastLoginAt', label: 'Last Login', render: v => v ? new Date(v).toLocaleString() : 'Never' },
]

export default function Users() {
  const [users, setUsers] = useState([])

  useEffect(() => {
    api.get('/api/admin/users').then(setUsers)
  }, [])

  const handleDelete = async (id) => {
    if (!confirm('Delete this user?')) return
    await api.del(`/api/admin/users/${id}`)
    setUsers(users.filter(u => u.id !== id))
  }

  const handleResetPassword = async (id) => {
    const result = await api.post(`/api/admin/users/${id}/reset-password`)
    alert(`New password: ${result.password}\n\nCopy it now — it won't be shown again.`)
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-white">Users</h1>
        <Link
          to="/users/new"
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition-colors"
        >
          Add User
        </Link>
      </div>

      <DataTable
        columns={columns}
        data={users}
        actions={(row) => (
          <div className="space-x-3">
            <Link to={`/users/${row.id}/edit`} className="text-indigo-400 hover:text-indigo-300">Edit</Link>
            <button onClick={() => handleResetPassword(row.id)} className="text-yellow-400 hover:text-yellow-300">Reset Pwd</button>
            <button onClick={() => handleDelete(row.id)} className="text-red-400 hover:text-red-300">Delete</button>
          </div>
        )}
      />
    </div>
  )
}
