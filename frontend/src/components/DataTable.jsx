import { EmptyState } from './ui'

export default function DataTable({ columns, data, actions, emptyTitle = 'No data', emptyDescription }) {
  const colCount = columns.length + (actions ? 1 : 0)

  return (
    <div className="overflow-x-auto rounded-lg border border-border bg-surface-1/40">
      <table className="min-w-full divide-y divide-border">
        <thead>
          <tr>
            {columns.map(col => (
              <th key={col.key} className="px-5 py-3 text-left stat-label">{col.label}</th>
            ))}
            {actions && (
              <th className="px-5 py-3 text-right stat-label">Actions</th>
            )}
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {data.map((row, i) => (
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
          {data.length === 0 && (
            <tr>
              <td colSpan={colCount} className="px-5 py-2">
                <EmptyState size="sm" title={emptyTitle} description={emptyDescription} />
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
