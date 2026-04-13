export default function DataTable({ columns, data, actions }) {
  return (
    <div className="overflow-x-auto rounded-xl border border-gray-700">
      <table className="min-w-full divide-y divide-gray-700">
        <thead className="bg-gray-800">
          <tr>
            {columns.map(col => (
              <th key={col.key} className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                {col.label}
              </th>
            ))}
            {actions && (
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-400 uppercase tracking-wider">
                Actions
              </th>
            )}
          </tr>
        </thead>
        <tbody className="bg-gray-800/50 divide-y divide-gray-700">
          {data.map((row, i) => (
            <tr key={row.id || i} className="hover:bg-gray-700/50 transition-colors">
              {columns.map(col => (
                <td key={col.key} className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                  {col.render ? col.render(row[col.key], row) : String(row[col.key] ?? '')}
                </td>
              ))}
              {actions && (
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm">
                  {actions(row)}
                </td>
              )}
            </tr>
          ))}
          {data.length === 0 && (
            <tr>
              <td colSpan={columns.length + (actions ? 1 : 0)} className="px-6 py-8 text-center text-gray-500">
                No data
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
