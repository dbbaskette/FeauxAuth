const VARIANT_CLASS = {
  default: 'badge',
  read: 'badge badge-read',
  write: 'badge badge-write',
  admin: 'badge badge-admin',
  info: 'badge badge-info',
  success: 'badge badge-success',
  danger: 'badge badge-danger',
}

const SEMANTIC = new Set(['read', 'write', 'admin', 'info', 'success', 'danger'])

export default function Badge({
  variant = 'default',
  dot,
  className = '',
  children,
  ...rest
}) {
  const showDot = dot !== undefined ? dot : SEMANTIC.has(variant)
  const classes = [
    VARIANT_CLASS[variant] || VARIANT_CLASS.default,
    className,
  ].filter(Boolean).join(' ')

  return (
    <span className={classes} {...rest}>
      {showDot && <span className="badge-dot" />}
      {children}
    </span>
  )
}
