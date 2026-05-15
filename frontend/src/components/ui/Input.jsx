import { forwardRef, useState } from 'react'

const EyeIcon = ({ shown }) => (
  <svg width="16" height="16" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
    {shown ? (
      <path d="M3.3 2.3a1 1 0 011.4 0l12 12a1 1 0 11-1.4 1.4l-1.7-1.7A8.9 8.9 0 0110 16c-4 0-7.3-2.6-9-6 .8-1.6 2-3 3.6-4.1L3.3 3.7a1 1 0 010-1.4zM10 6a4 4 0 014 4c0 .6-.1 1.1-.3 1.6L8.4 6.3A4 4 0 0110 6zm-3.7 1.7L8 9.4A2 2 0 0010 12c.3 0 .6-.1.9-.2l1.4 1.4A4 4 0 016 10c0-.8.2-1.6.6-2.3z"/>
    ) : (
      <path d="M10 4c-4 0-7.3 2.6-9 6 1.7 3.4 5 6 9 6s7.3-2.6 9-6c-1.7-3.4-5-6-9-6zm0 10a4 4 0 110-8 4 4 0 010 8zm0-2a2 2 0 100-4 2 2 0 000 4z"/>
    )}
  </svg>
)

const Input = forwardRef(function Input({
  label,
  type = 'text',
  iconLeft,
  iconRight,
  rightAction,
  className = '',
  id,
  ...rest
}, ref) {
  const [revealed, setRevealed] = useState(false)
  const isPassword = type === 'password'
  const effectiveType = isPassword && revealed ? 'text' : type

  const inputClasses = [
    'input',
    iconLeft ? 'has-icon-left' : '',
    iconRight || isPassword ? 'has-icon-right' : '',
    className,
  ].filter(Boolean).join(' ')

  const inputEl = (
    <input ref={ref} id={id} type={effectiveType} className={inputClasses} {...rest} />
  )

  const hasGroup = iconLeft || iconRight || isPassword

  return (
    <div>
      {label && <label className="field-label" htmlFor={id}>{label}</label>}
      {hasGroup ? (
        <div className="input-group">
          {iconLeft && <span className="input-icon-left">{iconLeft}</span>}
          {inputEl}
          {iconRight && !isPassword && (
            <span className="input-icon-right" onClick={rightAction}>{iconRight}</span>
          )}
          {isPassword && (
            <button
              type="button"
              className="input-icon-right"
              onClick={() => setRevealed(v => !v)}
              aria-label={revealed ? 'Hide password' : 'Show password'}
            >
              <EyeIcon shown={revealed} />
            </button>
          )}
        </div>
      ) : inputEl}
    </div>
  )
})

export default Input
