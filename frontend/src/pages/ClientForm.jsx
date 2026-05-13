import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { Badge, Button, Card, Input, Mono, useToast } from '../components/ui'

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

export default function ClientForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
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

  const copySecret = async () => {
    try {
      await navigator.clipboard.writeText(secret)
      toast.success('Client secret copied')
    } catch {
      toast.error('Copy failed — select the value and copy manually')
    }
  }

  if (secret) {
    return (
      <div className="max-w-2xl">
        <Card elev className="border-emerald-500/30">
          <div className="flex items-center gap-2 mb-1">
            <Badge variant="success">success</Badge>
          </div>
          <h2 className="text-h2 mt-2 mb-1">Client secret generated</h2>
          <p className="text-text-dim text-sm mb-5">Copy the secret now — it won't be shown again.</p>

          <div className="space-y-4">
            <div>
              <label className="field-label">Client ID</label>
              <div className="mono px-3.5 py-2.5 rounded bg-surface-0 border border-border text-text break-all text-sm">
                {form.clientId}
              </div>
            </div>
            <div>
              <label className="field-label">Client Secret</label>
              <div className="mono px-3.5 py-2.5 rounded bg-surface-0 border border-border text-text break-all text-sm">
                {secret}
              </div>
            </div>
          </div>

          <div className="mt-5 flex gap-2.5">
            <Button onClick={copySecret} size="sm">Copy Secret</Button>
            <Button variant="default" size="sm" onClick={() => navigate('/clients')}>Done</Button>
          </div>
        </Card>
      </div>
    )
  }

  return (
    <div className="max-w-2xl">
      <div className="mb-6">
        <div className="eyebrow">{isEdit ? 'Edit' : 'Register'}</div>
        <h1 className="text-h1 mt-1">{isEdit ? 'Edit Client' : 'Add Client'}</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <Card>
          <div className="space-y-4">
            <Input id="name" label="Client Name" value={form.name}
                   onChange={e => handleNameChange(e.target.value)} required />
            <Input id="clientId" label="Client ID" value={form.clientId}
                   onChange={e => setForm(f => ({ ...f, clientId: e.target.value }))}
                   required disabled={isEdit} className="mono" />
            <div>
              <label className="field-label" htmlFor="redirectUris">Redirect URIs (one per line)</label>
              <textarea id="redirectUris" value={form.redirectUris}
                        onChange={e => setForm(f => ({ ...f, redirectUris: e.target.value }))}
                        required rows={3}
                        className="input mono" style={{ resize: 'vertical' }} />
            </div>
            <Input id="allowedScopes" label="Allowed Scopes (space-separated)"
                   value={form.allowedScopes}
                   onChange={e => setForm(f => ({ ...f, allowedScopes: e.target.value }))} required
                   className="mono" />
          </div>
        </Card>

        <Card>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input id="accessTokenTtl" label="Access Token TTL (seconds)" type="number"
                   value={form.accessTokenTtl}
                   onChange={e => setForm(f => ({ ...f, accessTokenTtl: parseInt(e.target.value) || 0 }))} />
            <Input id="refreshTokenTtl" label="Refresh Token TTL (seconds)" type="number"
                   value={form.refreshTokenTtl}
                   onChange={e => setForm(f => ({ ...f, refreshTokenTtl: parseInt(e.target.value) || 0 }))} />
          </div>
          <div className="mt-4 flex flex-wrap gap-5">
            <Checkbox label="Require PKCE" checked={form.requirePkce}
                      onChange={e => setForm(f => ({ ...f, requirePkce: e.target.checked }))} />
            <Checkbox label="Require Consent" checked={form.requireConsent}
                      onChange={e => setForm(f => ({ ...f, requireConsent: e.target.checked }))} />
            <Checkbox label="Enabled" checked={form.enabled}
                      onChange={e => setForm(f => ({ ...f, enabled: e.target.checked }))} />
          </div>
        </Card>

        <div className="flex gap-2.5">
          <Button type="submit">{isEdit ? 'Save Changes' : 'Create Client'}</Button>
          {isEdit && (
            <Button type="button" variant="default" onClick={handleResetSecret}>
              Reset Secret
            </Button>
          )}
          <Button type="button" variant="ghost" onClick={() => navigate('/clients')}>Cancel</Button>
        </div>
      </form>
    </div>
  )
}
