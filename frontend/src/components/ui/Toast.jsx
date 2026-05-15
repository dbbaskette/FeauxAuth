import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'

const ToastContext = createContext(null)
const MAX_VISIBLE = 3
const DEFAULT_TTL = 2500

let nextId = 1

const TONE_CLASS = {
  default: 'badge',
  success: 'badge badge-success',
  error: 'badge badge-danger',
  info: 'badge badge-info',
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const timers = useRef(new Map())

  const dismiss = useCallback(id => {
    setToasts(list => list.filter(t => t.id !== id))
    const handle = timers.current.get(id)
    if (handle) {
      clearTimeout(handle)
      timers.current.delete(id)
    }
  }, [])

  const push = useCallback((message, opts = {}) => {
    const tone = opts.tone || 'default'
    const ttl = opts.ttl ?? DEFAULT_TTL
    const id = nextId++
    setToasts(list => {
      const next = [...list, { id, message, tone }]
      return next.length > MAX_VISIBLE ? next.slice(next.length - MAX_VISIBLE) : next
    })
    const handle = setTimeout(() => dismiss(id), ttl)
    timers.current.set(id, handle)
    return id
  }, [dismiss])

  const api = useRef({
    show: (msg, opts) => push(msg, opts),
    success: (msg, opts) => push(msg, { ...opts, tone: 'success' }),
    error: (msg, opts) => push(msg, { ...opts, tone: 'error' }),
    info: (msg, opts) => push(msg, { ...opts, tone: 'info' }),
    dismiss,
  })

  useEffect(() => () => {
    timers.current.forEach(clearTimeout)
    timers.current.clear()
  }, [])

  return (
    <ToastContext.Provider value={api.current}>
      {children}
      <ToastViewport toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  )
}

function ToastViewport({ toasts, onDismiss }) {
  if (toasts.length === 0) return null
  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-2 pointer-events-none">
      {toasts.map(t => (
        <div
          key={t.id}
          role="status"
          className="pointer-events-auto card-elev !p-3 flex items-center gap-3 min-w-[220px] shadow-elev animate-[fadeIn_.15s_ease]"
        >
          <span className={TONE_CLASS[t.tone] || TONE_CLASS.default}>
            <span className="badge-dot" />
            {t.tone === 'error' ? 'Error' : t.tone === 'success' ? 'Done' : t.tone === 'info' ? 'Info' : 'Notice'}
          </span>
          <span className="text-sm text-text">{t.message}</span>
          <button
            type="button"
            className="ml-auto text-text-mute hover:text-text"
            onClick={() => onDismiss(t.id)}
            aria-label="Dismiss"
          >
            <svg width="14" height="14" viewBox="0 0 20 20" fill="currentColor"><path d="M4.3 4.3a1 1 0 011.4 0L10 8.6l4.3-4.3a1 1 0 111.4 1.4L11.4 10l4.3 4.3a1 1 0 11-1.4 1.4L10 11.4l-4.3 4.3a1 1 0 11-1.4-1.4L8.6 10 4.3 5.7a1 1 0 010-1.4z"/></svg>
          </button>
        </div>
      ))}
    </div>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) {
    return {
      show: msg => console.warn('[toast] no provider:', msg),
      success: msg => console.warn('[toast] no provider:', msg),
      error: msg => console.warn('[toast] no provider:', msg),
      info: msg => console.warn('[toast] no provider:', msg),
      dismiss: () => {},
    }
  }
  return ctx
}
