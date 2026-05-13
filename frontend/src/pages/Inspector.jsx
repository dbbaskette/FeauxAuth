import { useState } from 'react'
import { api } from '../api/client'
import { Badge, Button, Card } from '../components/ui'

function StatusCard({ label, status, tone }) {
  const className = tone === 'good'
    ? 'border-emerald-500/30 bg-emerald-500/5'
    : tone === 'bad'
      ? 'border-rose-500/30 bg-rose-500/5'
      : 'border-border bg-surface-1'
  const text = tone === 'good'
    ? 'text-emerald-400'
    : tone === 'bad'
      ? 'text-rose-400'
      : 'text-text-dim'
  return (
    <div className={`rounded-lg border p-4 ${className}`}>
      <p className="stat-label">{label}</p>
      <p className={`mt-1.5 text-base font-semibold ${text}`}>{status}</p>
    </div>
  )
}

export default function Inspector() {
  const [token, setToken] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  const handleInspect = async (e) => {
    e.preventDefault()
    setError('')
    setResult(null)
    try {
      const data = await api.post('/api/admin/inspector', { token })
      if (data.error) setError(data.error)
      else setResult(data)
    } catch {
      setError('Failed to inspect token')
    }
  }

  return (
    <div className="max-w-4xl">
      <div className="mb-6">
        <div className="eyebrow">Debug</div>
        <h1 className="text-h1 mt-1">Token Inspector</h1>
        <p className="text-text-dim text-sm mt-1.5">Decode a JWT, verify its signature, and check revocation status.</p>
      </div>

      <Card className="mb-6">
        <form onSubmit={handleInspect}>
          <label className="field-label" htmlFor="jwt">JWT</label>
          <textarea
            id="jwt"
            value={token}
            onChange={e => setToken(e.target.value)}
            rows={4}
            placeholder="eyJhbGciOiJSUzI1NiIs…"
            className="input mono"
            style={{ fontSize: '12.5px', resize: 'vertical' }}
          />
          <div className="mt-3 flex justify-end">
            <Button type="submit" size="sm">Inspect Token</Button>
          </div>
        </form>
      </Card>

      {error && (
        <div className="rounded border border-rose-500/30 bg-rose-500/5 text-rose-300 px-3.5 py-2.5 text-sm mb-6">
          {error}
        </div>
      )}

      {result && (
        <div className="space-y-6">
          <div>
            <div className="flex items-center justify-between mb-2.5">
              <h2 className="text-h2">Header</h2>
              <Badge variant="info">decoded</Badge>
            </div>
            <pre className="card mono text-sm overflow-x-auto !bg-surface-0">
{JSON.stringify(result.header, null, 2)}
            </pre>
          </div>

          <div>
            <div className="flex items-center justify-between mb-2.5">
              <h2 className="text-h2">Payload</h2>
              <Badge variant="info">decoded</Badge>
            </div>
            <pre className="card mono text-sm overflow-x-auto !bg-surface-0">
{JSON.stringify(result.payload, null, 2)}
            </pre>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <StatusCard
              label="Signature"
              status={result.signatureValid ? 'Valid' : 'Invalid'}
              tone={result.signatureValid ? 'good' : 'bad'}
            />
            <StatusCard
              label="Expiry"
              status={result.expired ? 'Expired' : 'Valid'}
              tone={result.expired ? 'bad' : 'good'}
            />
            <StatusCard
              label="Revocation"
              status={
                result.revocationStatus === 'revoked' ? 'Revoked' :
                result.revocationStatus === 'active' ? 'Active' : 'Unknown'
              }
              tone={
                result.revocationStatus === 'revoked' ? 'bad' :
                result.revocationStatus === 'active' ? 'good' : 'neutral'
              }
            />
          </div>
        </div>
      )}
    </div>
  )
}
