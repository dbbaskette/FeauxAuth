const VARIANT_CLASS = {
  primary: 'btn btn-primary',
  default: 'btn',
  ghost: 'btn btn-ghost',
  danger: 'btn btn-danger',
}

export default function Button({
  variant = 'primary',
  size = 'md',
  block = false,
  as: Tag = 'button',
  className = '',
  type,
  children,
  ...rest
}) {
  const classes = [
    VARIANT_CLASS[variant] || VARIANT_CLASS.primary,
    size === 'sm' ? 'btn-sm' : '',
    block ? 'btn-block' : '',
    className,
  ].filter(Boolean).join(' ')

  const buttonType = Tag === 'button' ? (type || 'button') : type
  return (
    <Tag className={classes} type={buttonType} {...rest}>
      {children}
    </Tag>
  )
}
