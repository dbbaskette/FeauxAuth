import { useState, useEffect } from 'react'
import { api } from '../api/client'
import DataTable from '../components/DataTable'
import { tokenColumns } from '../components/tokenColumns'

const columns = [
  ...tokenColumns,
  { key: 'revoked', label: 'Status', render: v => v ? 'Revoked' : 'Active' },
]

export default function Tokens() {
  const [tokens, setTokens] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const loadTokens = async (p) => {
    const data = await api.get(`/api/admin/tokens?page=${p}&size=20`)
    setTokens(data.content || [])
    setTotalPages(data.totalPages || 0)
    setPage(p)
  }

  useEffect(() => { loadTokens(0) }, [])

  const handleRevoke = async (jti) => {
    await api.post(`/api/admin/tokens/${jti}/revoke`)
    loadTokens(page)
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-white mb-6">Active Tokens</h1>

      <DataTable
        columns={columns}
        data={tokens}
        actions={(row) => (
          !row.revoked && (
            <button onClick={() => handleRevoke(row.jti)} className="text-red-400 hover:text-red-300">
              Revoke
            </button>
          )
        )}
      />

      {totalPages > 1 && (
        <div className="flex justify-center space-x-2 mt-4">
          <button disabled={page === 0} onClick={() => loadTokens(page - 1)}
                  className="px-3 py-1 bg-gray-700 text-white rounded disabled:opacity-50">Prev</button>
          <span className="text-gray-400 py-1">Page {page + 1} of {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => loadTokens(page + 1)}
                  className="px-3 py-1 bg-gray-700 text-white rounded disabled:opacity-50">Next</button>
        </div>
      )}
    </div>
  )
}
