export default function EmptyState({
  icon,
  title,
  description,
  action,
  size = 'md',
  className = '',
}) {
  const padding = size === 'sm' ? 'py-8' : 'py-14'
  return (
    <div className={`flex flex-col items-center text-center ${padding} ${className}`}>
      <div className="w-14 h-14 rounded-full bg-primary-gradient-soft border border-border grid place-items-center text-text-dim mb-4">
        {icon || (
          <svg width="22" height="22" viewBox="0 0 20 20" fill="currentColor">
            <path d="M10 2a8 8 0 110 16 8 8 0 010-16zm0 6a1 1 0 00-1 1v4a1 1 0 102 0V9a1 1 0 00-1-1zm0-3a1 1 0 100 2 1 1 0 000-2z"/>
          </svg>
        )}
      </div>
      {title && <h3 className="text-text font-semibold text-base mb-1">{title}</h3>}
      {description && <p className="text-text-dim text-sm max-w-xs">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
