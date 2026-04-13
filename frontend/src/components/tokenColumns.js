export const tokenColumns = [
  { key: 'jti', label: 'Token ID', render: v => v?.substring(0, 8) + '...' },
  { key: 'clientId', label: 'Client' },
  { key: 'userId', label: 'User', render: v => v?.substring(0, 8) + '...' },
  { key: 'scope', label: 'Scopes' },
  { key: 'createdAt', label: 'Issued', render: v => new Date(v).toLocaleString() },
  { key: 'expiresAt', label: 'Expires', render: v => new Date(v).toLocaleString() },
]
