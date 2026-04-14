import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'

export default function ClientForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = !!id

  const [form, setForm] = useState({
    name: '',
    clientId: '',
    redirectUris: '',
    allowedScopes: 'openid profile email',
    accessTokenTtl: 3600,
    refreshTokenTtl: 2592000,
    requirePkce: false,
    requireConsent: false,
    enabled: true,
  })
  const [secret, setSecret] = useState(null)

  useEffect(() => {
    if (isEdit) {
      api.get(`/api/admin/clients/${id}`).then(data => {
        setForm({
          name: data.name,
          clientId: data.clientId,
          redirectUris: data.redirectUris,
          allowedScopes: data.allowedScopes,
          accessTokenTtl: data.accessTokenTtl,
          refreshTokenTtl: data.refreshTokenTtl,
          requirePkce: data.requirePkce,
          requireConsent: data.requireConsent || false,
          enabled: data.enabled,
        })
      })
    }
  }, [id, isEdit])

  const handleNameChange = (name) => {
    setForm(f => ({
      ...f,
      name,
      clientId: isEdit ? f.clientId : name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, ''),
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (isEdit) {
      await api.put(`/api/admin/clients/${id}`, form)
      navigate('/clients')
    } else {
      const result = await api.post('/api/admin/clients', form)
      setSecret(result.clientSecret)
    }
  }

  const handleResetSecret = async () => {
    const result = await api.post(`/api/admin/clients/${id}/reset-secret`)
    setSecret(result.clientSecret)
  }

  if (secret) {
    return (
      <div className="max-w-2xl">
        <div className="bg-green-900/50 border border-green-700 rounded-xl p-6">
          <h2 className="text-lg font-bold text-green-300 mb-4">Client Created Successfully</h2>
          <p className="text-gray-300 mb-2">Copy the client secret now — it won't be shown again.</p>
          <div className="space-y-3">
            <div>
              <label className="text-sm text-gray-400">Client ID</label>
              <div className="font-mono bg-gray-800 px-4 py-2 rounded-lg text-white">{form.clientId}</div>
            </div>
            <div>
              <label className="text-sm text-gray-400">Client Secret</label>
              <div className="font-mono bg-gray-800 px-4 py-2 rounded-lg text-white break-all">{secret}</div>
            </div>
            <button
              onClick={() => navigator.clipboard.writeText(secret)}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm transition-colors"
            >
              Copy Secret
            </button>
            <button
              onClick={() => navigate('/clients')}
              className="ml-3 px-4 py-2 bg-gray-600 hover:bg-gray-500 text-white rounded-lg text-sm transition-colors"
            >
              Done
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold text-white mb-6">{isEdit ? 'Edit Client' : 'Add Client'}</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Client Name</label>
          <input type="text" value={form.name} onChange={e => handleNameChange(e.target.value)} required
                 className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Client ID</label>
          <input type="text" value={form.clientId} onChange={e => setForm(f => ({ ...f, clientId: e.target.value }))}
                 required disabled={isEdit}
                 className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50" />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Redirect URIs (one per line)</label>
          <textarea value={form.redirectUris} onChange={e => setForm(f => ({ ...f, redirectUris: e.target.value }))}
                    required rows={3}
                    className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Allowed Scopes (space-separated)</label>
          <input type="text" value={form.allowedScopes} onChange={e => setForm(f => ({ ...f, allowedScopes: e.target.value }))} required
                 className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Access Token TTL (seconds)</label>
            <input type="number" value={form.accessTokenTtl} onChange={e => setForm(f => ({ ...f, accessTokenTtl: parseInt(e.target.value) }))}
                   className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Refresh Token TTL (seconds)</label>
            <input type="number" value={form.refreshTokenTtl} onChange={e => setForm(f => ({ ...f, refreshTokenTtl: parseInt(e.target.value) }))}
                   className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
        </div>

        <div className="flex items-center space-x-6">
          <label className="flex items-center space-x-2">
            <input type="checkbox" checked={form.requirePkce} onChange={e => setForm(f => ({ ...f, requirePkce: e.target.checked }))}
                   className="rounded bg-gray-700 border-gray-600 text-indigo-500 focus:ring-indigo-500" />
            <span className="text-sm text-gray-300">Require PKCE</span>
          </label>
          <label className="flex items-center space-x-2">
            <input type="checkbox" checked={form.requireConsent} onChange={e => setForm(f => ({ ...f, requireConsent: e.target.checked }))}
                   className="rounded bg-gray-700 border-gray-600 text-indigo-500 focus:ring-indigo-500" />
            <span className="text-sm text-gray-300">Require Consent</span>
          </label>
          <label className="flex items-center space-x-2">
            <input type="checkbox" checked={form.enabled} onChange={e => setForm(f => ({ ...f, enabled: e.target.checked }))}
                   className="rounded bg-gray-700 border-gray-600 text-indigo-500 focus:ring-indigo-500" />
            <span className="text-sm text-gray-300">Enabled</span>
          </label>
        </div>

        <div className="flex space-x-3">
          <button type="submit"
                  className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors">
            {isEdit ? 'Save Changes' : 'Create Client'}
          </button>
          {isEdit && (
            <button type="button" onClick={handleResetSecret}
                    className="px-6 py-3 bg-yellow-600 hover:bg-yellow-700 text-white font-medium rounded-lg transition-colors">
              Reset Secret
            </button>
          )}
          <button type="button" onClick={() => navigate('/clients')}
                  className="px-6 py-3 bg-gray-600 hover:bg-gray-500 text-white font-medium rounded-lg transition-colors">
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
