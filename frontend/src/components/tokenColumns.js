import { createElement } from 'react'
import Mono from './ui/Mono'
import ScopeBadge from './ui/ScopeBadge'

const truncated = (v) =>
  v ? createElement(Mono, null, v.substring(0, 8) + '…') : ''

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
  { key: 'jti', label: 'Token ID', render: truncated },
  { key: 'clientId', label: 'Client' },
  { key: 'userId', label: 'User', render: truncated },
  { key: 'scope', label: 'Scopes', render: scopeList },
  { key: 'createdAt', label: 'Issued', render: ts },
  { key: 'expiresAt', label: 'Expires', render: ts },
]
