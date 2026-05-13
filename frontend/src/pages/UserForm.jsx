import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { Button, Card, Input } from '../components/ui'

function Checkbox({ label, checked, onChange }) {
  return (
    <label className="inline-flex items-center gap-2 cursor-pointer select-none">
      <input
        type="checkbox"
        checked={checked}
        onChange={onChange}
        className="w-4 h-4 rounded bg-surface-0 border-border-strong text-violet-500 focus:ring-violet-500"
      />
      <span className="text-sm text-text-dim">{label}</span>
    </label>
  )
}

export default function UserForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = !!id

  const [form, setForm] = useState({
    email: '',
    displayName: '',
    password: '',
    roles: '',
    enabled: true,
  })

  useEffect(() => {
    if (isEdit) {
      api.get(`/api/admin/users/${id}`).then(data => {
        setForm({
          email: data.email,
          displayName: data.displayName,
          password: '',
          roles: data.roles || '',
          enabled: data.enabled,
        })
      })
    }
  }, [id, isEdit])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (isEdit) {
      const { password, ...updateData } = form
      await api.put(`/api/admin/users/${id}`, updateData)
    } else {
      await api.post('/api/admin/users', form)
    }
    navigate('/users')
  }

  return (
    <div className="max-w-2xl">
      <div className="mb-6">
        <div className="eyebrow">{isEdit ? 'Edit' : 'Add'}</div>
        <h1 className="text-h1 mt-1">{isEdit ? 'Edit User' : 'Add User'}</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <Card>
          <div className="space-y-4">
            <Input id="email" label="Email" type="email"
                   value={form.email}
                   onChange={e => setForm(f => ({ ...f, email: e.target.value }))} required />
            <Input id="displayName" label="Display Name"
                   value={form.displayName}
                   onChange={e => setForm(f => ({ ...f, displayName: e.target.value }))} required />
            <Input id="roles" label="Roles (comma-separated)"
                   value={form.roles}
                   onChange={e => setForm(f => ({ ...f, roles: e.target.value }))}
                   placeholder="admin,editor,viewer" />
            {!isEdit && (
              <Input id="password" label="Password (min 8 characters)" type="password"
                     value={form.password}
                     onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                     required minLength={8} />
            )}
            <Checkbox label="Enabled" checked={form.enabled}
                      onChange={e => setForm(f => ({ ...f, enabled: e.target.checked }))} />
          </div>
        </Card>

        <div className="flex gap-2.5">
          <Button type="submit">{isEdit ? 'Save Changes' : 'Create User'}</Button>
          <Button type="button" variant="ghost" onClick={() => navigate('/users')}>Cancel</Button>
        </div>
      </form>
    </div>
  )
}
