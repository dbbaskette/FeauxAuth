import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'

export default function UserForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = !!id

  const [form, setForm] = useState({
    email: '',
    displayName: '',
    password: '',
    enabled: true,
  })

  useEffect(() => {
    if (isEdit) {
      api.get(`/api/admin/users/${id}`).then(data => {
        setForm({
          email: data.email,
          displayName: data.displayName,
          password: '',
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
      <h1 className="text-2xl font-bold text-white mb-6">{isEdit ? 'Edit User' : 'Add User'}</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Email</label>
          <input type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                 required
                 className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Display Name</label>
          <input type="text" value={form.displayName} onChange={e => setForm(f => ({ ...f, displayName: e.target.value }))}
                 required
                 className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        </div>

        {!isEdit && (
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Password (min 8 characters)</label>
            <input type="password" value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                   required minLength={8}
                   className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
        )}

        <label className="flex items-center space-x-2">
          <input type="checkbox" checked={form.enabled} onChange={e => setForm(f => ({ ...f, enabled: e.target.checked }))}
                 className="rounded bg-gray-700 border-gray-600 text-indigo-500 focus:ring-indigo-500" />
          <span className="text-sm text-gray-300">Enabled</span>
        </label>

        <div className="flex space-x-3">
          <button type="submit"
                  className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors">
            {isEdit ? 'Save Changes' : 'Create User'}
          </button>
          <button type="button" onClick={() => navigate('/users')}
                  className="px-6 py-3 bg-gray-600 hover:bg-gray-500 text-white font-medium rounded-lg transition-colors">
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
