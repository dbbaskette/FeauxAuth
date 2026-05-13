import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import { Badge, Button, Card, CopyableMono, Mono, ScopeBadge, useToast } from '../components/ui'

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

function JsonBlock({ title, value }) {
  const toast = useToast()
  const text = JSON.stringify(value, null, 2)
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text)
      toast.success(`${title} copied`)
    } catch {
      toast.error('Copy failed')
    }
  }
  return (
    <div>
      <div className="flex items-center justify-between mb-2.5">
        <h2 className="text-h2">{title}</h2>
        <div className="flex items-center gap-2">
          <Badge variant="info">decoded</Badge>
          <Button variant="ghost" size="sm" onClick={copy}>Copy JSON</Button>
        </div>
      </div>
      <pre className="card mono text-sm overflow-x-auto !bg-surface-0">
{text}
      </pre>
    </div>
  )
}

function MetadataView({ token, isExpired }) {
  const tone = token.revoked ? 'bad' : isExpired ? 'bad' : 'good'
  const status = token.revoked ? 'Revoked' : isExpired ? 'Expired' : 'Active'
  return (
    <div className="space-y-5">
      <div className="rounded border border-violet-500/30 bg-violet-500/5 px-3.5 py-2.5">
        <div className="eyebrow text-violet-300 mb-1">Metadata mode</div>
        <div className="text-sm text-text-dim">
          Showing stored claims for this token. Signature and full payload aren't available without the original JWT — paste it below for full validation.
        </div>
      </div>

      <Card>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
          <div>
            <div className="field-label">JTI</div>
            <CopyableMono>{token.jti}</CopyableMono>
          </div>
          <div>
            <div className="field-label">Status</div>
            <Badge variant={tone === 'good' ? 'success' : 'danger'}>{status}</Badge>
          </div>
          <div>
            <div className="field-label">Client</div>
            <Mono className="text-text">{token.clientId}</Mono>
          </div>
          <div>
            <div className="field-label">User</div>
            {token.userId
              ? <CopyableMono>{token.userId}</CopyableMono>
              : <span className="text-text-mute text-sm">— (client_credentials)</span>}
          </div>
          <div className="md:col-span-2">
            <div className="field-label">Scopes</div>
            <div className="flex flex-wrap gap-1">
              {(token.scope || '').split(/\s+/).filter(Boolean).map(s => <ScopeBadge key={s} scope={s} />)}
            </div>
          </div>
          <div>
            <div className="field-label">Issued</div>
            <div className="text-sm text-text">{new Date(token.createdAt).toLocaleString()}</div>
          </div>
          <div>
            <div className="field-label">Expires</div>
            <div className="text-sm text-text">{new Date(token.expiresAt).toLocaleString()}</div>
          </div>
        </div>
      </Card>
    </div>
  )
}

export default function Inspector() {
  const [searchParams, setSearchParams] = useSearchParams()
  const initialJti = searchParams.get('jti') || ''
  const [token, setToken] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [metadata, setMetadata] = useState(null)
  const [metadataError, setMetadataError] = useState('')

  useEffect(() => {
    if (!initialJti) return
    setMetadata(null)
    setMetadataError('')
    setResult(null)
    setError('')
    api.get(`/api/admin/tokens/${encodeURIComponent(initialJti)}`)
      .then(data => {
        if (data && data.jti) setMetadata(data)
        else setMetadataError('Token not found')
      })
      .catch(() => setMetadataError('Token not found'))
  }, [initialJti])

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

  const clearMetadata = () => {
    setMetadata(null)
    setMetadataError('')
    setSearchParams({})
  }

  const isMetadataExpired = metadata?.expiresAt
    ? new Date(metadata.expiresAt).getTime() < Date.now()
    : false

  return (
    <div className="max-w-4xl">
      <div className="mb-6">
        <div className="eyebrow">Debug</div>
        <h1 className="text-h1 mt-1">Token Inspector</h1>
        <p className="text-text-dim text-sm mt-1.5">Decode a JWT, verify its signature, and check revocation status.</p>
      </div>

      {metadataError && (
        <div className="rounded border border-rose-500/30 bg-rose-500/5 text-rose-300 px-3.5 py-2.5 text-sm mb-6 flex items-center justify-between">
          <span>{metadataError}</span>
          <button onClick={clearMetadata} className="text-text-dim hover:text-text">×</button>
        </div>
      )}

      {metadata && (
        <div className="mb-6">
          <MetadataView token={metadata} isExpired={isMetadataExpired} />
          <div className="mt-3 flex justify-end">
            <Button variant="ghost" size="sm" onClick={clearMetadata}>Clear metadata view</Button>
          </div>
        </div>
      )}

      <Card className="mb-6">
        <form onSubmit={handleInspect}>
          <label className="field-label" htmlFor="jwt">{metadata ? 'Paste the full JWT for signature verification' : 'JWT'}</label>
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
          <JsonBlock title="Header" value={result.header} />
          <JsonBlock title="Payload" value={result.payload} />

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
