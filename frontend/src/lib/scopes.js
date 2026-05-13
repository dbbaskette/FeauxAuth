// Scope risk classification — mirrored in src/main/java/.../oauth/ScopeRisk.java
// Keep the two implementations in lockstep. See:
//   docs/superpowers/specs/2026-05-13-aurora-design-system-design.md

const STANDARD_READ_SCOPES = new Set([
  'openid', 'profile', 'email', 'address', 'phone',
])

const WRITE_SUFFIXES = ['.write', '.send', '.modify', '.create', '.delete']

export function classifyScope(scope) {
  if (!scope || typeof scope !== 'string') return 'read'
  const s = scope.trim().toLowerCase()
  if (s.length === 0) return 'read'

  if (s === 'offline_access') return 'info'
  if (STANDARD_READ_SCOPES.has(s)) return 'read'

  if (s.includes('admin') || s.includes('manage') || s.endsWith('.manage')) return 'admin'
  for (const suffix of WRITE_SUFFIXES) {
    if (s.endsWith(suffix)) return 'write'
  }
  return 'read'
}

export const SCOPE_RISK_LABELS = {
  read: 'Read',
  write: 'Write',
  admin: 'Admin',
  info: 'Info',
}

export function classifyScopes(scopes) {
  if (!scopes) return []
  const list = Array.isArray(scopes) ? scopes : String(scopes).split(/\s+/)
  return list
    .filter(Boolean)
    .map(scope => ({ scope, risk: classifyScope(scope) }))
}
