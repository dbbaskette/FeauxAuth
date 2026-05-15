const SearchIcon = () => (
  <svg width="14" height="14" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
    <path d="M9 2a7 7 0 015.3 11.6l4 4a1 1 0 11-1.4 1.4l-4-4A7 7 0 119 2zm0 2a5 5 0 100 10A5 5 0 009 4z"/>
  </svg>
)

export default function TableToolbar({
  search,
  onSearchChange,
  searchPlaceholder = 'Search…',
  filters = [],
  filterValues = {},
  onFilterChange,
  rightSlot,
}) {
  if (search === undefined && filters.length === 0 && !rightSlot) return null
  return (
    <div className="flex items-center gap-2.5 mb-3">
      {search !== undefined && (
        <div className="flex-1 input-group">
          <span className="input-icon-left"><SearchIcon /></span>
          <input
            className="input has-icon-left"
            placeholder={searchPlaceholder}
            value={search}
            onChange={e => onSearchChange?.(e.target.value)}
          />
        </div>
      )}
      {filters.map(f => (
        <select
          key={f.key}
          value={filterValues[f.key] ?? '_all'}
          onChange={e => onFilterChange?.(f.key, e.target.value)}
          className="input !py-2 !px-3 text-sm"
          style={{ width: 'auto', minWidth: '140px' }}
        >
          <option value="_all">{f.label}: All</option>
          {f.options.map(o => (
            <option key={o.value} value={o.value}>{f.label}: {o.label}</option>
          ))}
        </select>
      ))}
      {rightSlot && <div className="ml-auto">{rightSlot}</div>}
    </div>
  )
}
