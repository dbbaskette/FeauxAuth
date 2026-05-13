import { useToast } from './Toast'

const CopyIcon = () => (
  <svg width="12" height="12" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
    <path d="M5 3a2 2 0 012-2h6a2 2 0 012 2v1h1a2 2 0 012 2v9a2 2 0 01-2 2H8a2 2 0 01-2-2v-1H5a2 2 0 01-2-2V5a2 2 0 012-2zm2 0v8h6V3H7zm0 10v1a2 2 0 002 2h6a2 2 0 002-2V6h-1v7a2 2 0 01-2 2H7z"/>
  </svg>
)

export default function CopyableMono({
  children,
  displayValue,
  toastMessage,
  className = '',
  iconClassName = '',
}) {
  const toast = useToast()
  const value = String(children ?? '')
  const display = displayValue ?? value

  const onCopy = async (e) => {
    e.preventDefault()
    e.stopPropagation()
    if (!navigator.clipboard) {
      toast.error('Copy not available in this browser')
      return
    }
    try {
      await navigator.clipboard.writeText(value)
      toast.success(toastMessage || `Copied: ${display.length > 24 ? display.slice(0, 24) + '…' : display}`)
    } catch {
      toast.error('Copy failed')
    }
  }

  return (
    <span className={`inline-flex items-center gap-1.5 group ${className}`}>
      <span className="mono">{display}</span>
      <button
        type="button"
        onClick={onCopy}
        title="Copy"
        aria-label={`Copy ${display}`}
        className={`opacity-50 hover:opacity-100 text-text-mute hover:text-violet-400 transition-opacity p-0.5 ${iconClassName}`}
      >
        <CopyIcon />
      </button>
    </span>
  )
}
