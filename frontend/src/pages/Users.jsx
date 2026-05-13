import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import DataTable from '../components/DataTable'
import { Badge, Button, useToast } from '../components/ui'

const columns = [
  { key: 'email', label: 'Email' },
  { key: 'displayName', label: 'Display Name' },
  {
    key: 'enabled',
    label: 'Status',
    render: v => v
      ? <Badge variant="success">Active</Badge>
      : <Badge variant="danger">Disabled</Badge>,
  },
  {
    key: 'lastLoginAt',
    label: 'Last Login',
    render: v => v ? new Date(v).toLocaleString() : <span className="text-text-mute">Never</span>,
  },
]

export default function Users() {
  const [users, setUsers] = useState([])
  const toast = useToast()

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
    try {
      await navigator.clipboard.writeText(result.password)
      toast.success(`Temporary password copied to clipboard. Share it securely — it won't be shown again.`)
    } catch {
      alert(`New password: ${result.password}\n\nCopy it now — it won't be shown again.`)
    }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <div className="eyebrow">Manage</div>
          <h1 className="text-h1 mt-1">Users</h1>
        </div>
        <Button as={Link} to="/users/new" size="sm">+ Add User</Button>
      </div>

      <DataTable
        columns={columns}
        data={users}
        emptyTitle="No users yet"
        emptyDescription="Add your first user to enable interactive OAuth flows."
        actions={(row) => (
          <div className="inline-flex items-center gap-3">
            <Link to={`/users/${row.id}/edit`} className="text-violet-400 hover:text-violet-300 text-sm font-medium">Edit</Link>
            <button onClick={() => handleResetPassword(row.id)} className="text-amber-400 hover:text-amber-300 text-sm font-medium">Reset Pwd</button>
            <button onClick={() => handleDelete(row.id)} className="text-rose-400 hover:text-rose-300 text-sm font-medium">Delete</button>
          </div>
        )}
      />
    </div>
  )
}
