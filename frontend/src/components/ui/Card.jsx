export default function Card({
  elev = false,
  padded = false,
  className = '',
  header,
  footer,
  children,
  ...rest
}) {
  const classes = [
    elev ? 'card-elev' : 'card',
    padded ? 'card-padded' : '',
    className,
  ].filter(Boolean).join(' ')

  return (
    <div className={classes} {...rest}>
      {header && <div className="mb-4">{header}</div>}
      {children}
      {footer && <div className="mt-4">{footer}</div>}
    </div>
  )
}
