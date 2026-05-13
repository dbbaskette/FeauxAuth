import { createElement } from 'react'
import { Link } from 'react-router-dom'
import CopyableMono from './ui/CopyableMono'
import ScopeBadge from './ui/ScopeBadge'

const truncatedJti = (v) => {
  if (!v) return ''
  return createElement(
    Link,
    {
      to: `/inspector?jti=${encodeURIComponent(v)}`,
      className: 'mono text-text-dim hover:text-violet-300 hover:underline transition-colors',
      title: 'Inspect this token',
    },
    v.substring(0, 8) + '…'
  )
}

const truncatedCopyable = (v) => {
  if (!v) return ''
  return createElement(CopyableMono, { displayValue: v.substring(0, 8) + '…' }, v)
}

const scopeList = (v) => {
  const parts = (v || '').split(/\s+/).filter(Boolean)
  if (parts.length === 0) return ''
  return createElement(
    'div',
    { className: 'flex flex-wrap gap-1' },
    parts.map(s => createElement(ScopeBadge, { key: s, scope: s }))
  )
}

const ts = (v) => v ? new Date(v).toLocaleString() : ''

export const tokenColumns = [
  { key: 'jti', label: 'Token ID', render: truncatedJti },
  { key: 'clientId', label: 'Client', sortable: true },
  { key: 'userId', label: 'User', render: truncatedCopyable },
  { key: 'scope', label: 'Scopes', render: scopeList },
  { key: 'createdAt', label: 'Issued', render: ts, sortable: true },
  { key: 'expiresAt', label: 'Expires', render: ts, sortable: true },
]
