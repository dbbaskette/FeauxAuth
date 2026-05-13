const DELTA_TONE = {
  positive: 'text-[#34d399]',
  warning: 'text-[#fbbf24]',
  neutral: 'text-text-mute',
}

export default function StatCard({ label, value, icon, delta }) {
  let deltaText, deltaTone
  if (delta) {
    if (typeof delta === 'string') { deltaText = delta; deltaTone = 'positive' }
    else { deltaText = delta.text; deltaTone = delta.tone || 'positive' }
  }
  return (
    <div className="card relative">
      {icon && (
        <span className="absolute top-4 right-4 w-8 h-8 rounded-md grid place-items-center text-text-dim border border-border bg-primary-gradient-soft">
          {icon}
        </span>
      )}
      <p className="stat-label">{label}</p>
      <p className="mt-2 text-[32px] font-bold tracking-tight leading-none text-text">{value}</p>
      {deltaText && (
        <p className={`mt-2.5 text-xs font-medium ${DELTA_TONE[deltaTone] || DELTA_TONE.positive}`}>
          {deltaText}
        </p>
      )}
    </div>
  )
}
