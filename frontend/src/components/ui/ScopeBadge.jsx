import { classifyScope } from '../../lib/scopes'
import Badge from './Badge'

export default function ScopeBadge({ scope, showRiskLabel = false, className = '' }) {
  const risk = classifyScope(scope)
  return (
    <Badge variant={risk} className={className}>
      {showRiskLabel ? `${risk.toUpperCase()} · ${scope}` : scope}
    </Badge>
  )
}
