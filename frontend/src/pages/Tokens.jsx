import { useState, useEffect } from 'react'
import { api } from '../api/client'
import DataTable from '../components/DataTable'
import { tokenColumns } from '../components/tokenColumns'
import { Badge, Button } from '../components/ui'

const columns = [
  ...tokenColumns,
  {
    key: 'revoked',
    label: 'Status',
    sortable: true,
    render: v => v
      ? <Badge variant="danger">Revoked</Badge>
      : <Badge variant="success">Active</Badge>,
  },
]

const filters = [
  {
    key: 'revoked',
    label: 'Status',
    options: [
      { value: 'false', label: 'Active' },
      { value: 'true', label: 'Revoked' },
    ],
  },
]

export default function Tokens() {
  const [tokens, setTokens] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const loadTokens = async (p) => {
    setLoading(true)
    try {
      const data = await api.get(`/api/admin/tokens?page=${p}&size=20`)
      setTokens(data.content || [])
      setTotalPages(data.totalPages || 0)
      setPage(p)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadTokens(0) }, [])

  const handleRevoke = async (jti) => {
    await api.post(`/api/admin/tokens/${jti}/revoke`)
    loadTokens(page)
  }

  return (
    <div>
      <div className="mb-6">
        <div className="eyebrow">Monitor</div>
        <h1 className="text-h1 mt-1">Active Tokens</h1>
      </div>

      <DataTable
        columns={columns}
        data={tokens}
        loading={loading}
        searchableKeys={['jti', 'clientId', 'userId', 'scope']}
        filters={filters}
        defaultSort={{ key: 'createdAt', dir: 'desc' }}
        emptyTitle="No tokens issued"
        emptyDescription="Once a client mints a token, it'll appear here."
        actions={(row) => (
          !row.revoked && (
            <button onClick={() => handleRevoke(row.jti)} className="text-rose-400 hover:text-rose-300 text-sm font-medium">
              Revoke
            </button>
          )
        )}
      />

      {totalPages > 1 && (
        <div className="flex justify-center items-center gap-3 mt-5">
          <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => loadTokens(page - 1)}>‹ Prev</Button>
          <span className="text-text-mute text-sm">Page {page + 1} of {totalPages}</span>
          <Button variant="ghost" size="sm" disabled={page >= totalPages - 1} onClick={() => loadTokens(page + 1)}>Next ›</Button>
        </div>
      )}
    </div>
  )
}
