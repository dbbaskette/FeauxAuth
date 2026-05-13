import { useMemo, useState } from 'react'
import { EmptyState, Skeleton, TableToolbar } from './ui'

const SortIcon = ({ dir }) => (
  <svg width="10" height="10" viewBox="0 0 12 12" fill="currentColor" className="inline-block ml-1 opacity-60">
    {dir === 'asc'
      ? <path d="M6 3l4 5H2l4-5z"/>
      : dir === 'desc'
        ? <path d="M6 9L2 4h8L6 9z"/>
        : <path d="M6 2l3 4H3l3-4zm0 8L3 6h6l-3 4z" opacity="0.6"/>}
  </svg>
)

function valueCompare(a, b) {
  if (a === b) return 0
  if (a == null) return 1
  if (b == null) return -1
  if (typeof a === 'number' && typeof b === 'number') return a - b
  return String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: 'base' })
}

export default function DataTable({
  columns,
  data,
  actions,
  loading = false,
  skeletonRows = 5,
  searchableKeys,
  filters,
  defaultSort,
  emptyTitle = 'No data',
  emptyDescription,
}) {
  const [search, setSearch] = useState('')
  const [filterValues, setFilterValues] = useState({})
  const [sort, setSort] = useState(defaultSort || null)

  const colCount = columns.length + (actions ? 1 : 0)

  const view = useMemo(() => {
    let out = data || []
    if (search && searchableKeys && searchableKeys.length > 0) {
      const q = search.toLowerCase()
      out = out.filter(row =>
        searchableKeys.some(k => {
          const v = row[k]
          return v != null && String(v).toLowerCase().includes(q)
        })
      )
    }
    for (const [k, v] of Object.entries(filterValues)) {
      if (v && v !== '_all') {
        out = out.filter(row => String(row[k]) === v)
      }
    }
    if (sort && sort.key) {
      const factor = sort.dir === 'desc' ? -1 : 1
      out = [...out].sort((a, b) => factor * valueCompare(a[sort.key], b[sort.key]))
    }
    return out
  }, [data, search, searchableKeys, filterValues, sort])

  const toggleSort = (key) => {
    setSort(prev => {
      if (!prev || prev.key !== key) return { key, dir: 'asc' }
      if (prev.dir === 'asc') return { key, dir: 'desc' }
      return null
    })
  }

  const showToolbar = searchableKeys?.length > 0 || (filters && filters.length > 0)

  return (
    <div>
      {showToolbar && (
        <TableToolbar
          search={searchableKeys?.length > 0 ? search : undefined}
          onSearchChange={setSearch}
          searchPlaceholder={`Search ${columns[0]?.label?.toLowerCase() || ''}…`}
          filters={filters || []}
          filterValues={filterValues}
          onFilterChange={(k, v) => setFilterValues(prev => ({ ...prev, [k]: v }))}
        />
      )}
      <div className="overflow-x-auto rounded-lg border border-border bg-surface-1/40">
        <table className="min-w-full divide-y divide-border">
          <thead>
            <tr>
              {columns.map(col => {
                const isSorted = sort?.key === col.key
                const sortable = col.sortable
                return (
                  <th
                    key={col.key}
                    className={`px-5 py-3 text-left stat-label ${sortable ? 'cursor-pointer select-none hover:text-text-dim' : ''}`}
                    onClick={sortable ? () => toggleSort(col.key) : undefined}
                  >
                    {col.label}
                    {sortable && <SortIcon dir={isSorted ? sort.dir : null} />}
                  </th>
                )
              })}
              {actions && (
                <th className="px-5 py-3 text-right stat-label">Actions</th>
              )}
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {loading && (data || []).length === 0 && Array.from({ length: skeletonRows }).map((_, i) => (
              <tr key={`sk-${i}`}>
                {columns.map(col => (
                  <td key={col.key} className="px-5 py-3.5">
                    <Skeleton width={`${50 + ((i * 13 + col.key.length * 7) % 40)}%`} height={10} />
                  </td>
                ))}
                {actions && <td className="px-5 py-3.5"><Skeleton width={60} height={10} /></td>}
              </tr>
            ))}
            {!loading && view.map((row, i) => (
              <tr key={row.id || row.jti || i} className="hover:bg-surface-2 transition-colors">
                {columns.map(col => (
                  <td key={col.key} className="px-5 py-3.5 whitespace-nowrap text-sm text-text-dim">
                    {col.render ? col.render(row[col.key], row) : String(row[col.key] ?? '')}
                  </td>
                ))}
                {actions && (
                  <td className="px-5 py-3.5 whitespace-nowrap text-right text-sm">
                    {actions(row)}
                  </td>
                )}
              </tr>
            ))}
            {!loading && view.length === 0 && (
              <tr>
                <td colSpan={colCount} className="px-5 py-2">
                  <EmptyState
                    size="sm"
                    title={search || Object.values(filterValues).some(v => v && v !== '_all')
                      ? 'No matches'
                      : emptyTitle}
                    description={search || Object.values(filterValues).some(v => v && v !== '_all')
                      ? 'Try clearing the search or filters.'
                      : emptyDescription}
                  />
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
