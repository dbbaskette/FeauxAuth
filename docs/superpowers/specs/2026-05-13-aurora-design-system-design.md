# Aurora — FeauxAuth Design System & Visual Refresh

**Sub-project:** 1 of 3 (foundation for OAuth flow UX overhaul and admin SPA polish)
**Date:** 2026-05-13
**Status:** Approved direction; spec for implementation

## Goals

Establish a single visual language that both surfaces of FeauxAuth share — the React admin SPA (`frontend/`) and the server-rendered OAuth flow templates (`src/main/resources/templates/oauth/`). Replace today's stock Tailwind dark theme with a refined "Aurora" identity: dev-tool aesthetic, high-contrast typography, and a small set of reusable primitives that downstream sub-projects (OAuth flow UX, admin SPA polish) will compose without redesign.

This sub-project ships the *system*, not new features. It re-skins what already exists and provides the primitives that #1–#7 will use.

## Non-Goals

- No light theme (dark-only for v1; light is a follow-up if needed).
- No icon library swap — keep inline SVGs already in use; introduce a small icon helper only if needed.
- No new pages, no behavior changes. Pure visual + primitives.
- No CSS-in-JS migration; stay on Tailwind + a small set of `@layer components` classes.
- Both surfaces continue to use Tailwind. We do not introduce a separate CSS pipeline for the Thymeleaf templates.

## Design Tokens

All tokens land in `tailwind.config.js` (`theme.extend`) so both the React SPA and the Thymeleaf templates (which load Tailwind via `<script src="https://cdn.tailwindcss.com">`) can consume them. The CDN config is configured inline in a `<script>` block in each template's `<head>` — see "Thymeleaf integration" below.

### Color palette

| Token | Hex | Use |
|---|---|---|
| `bg` | `#07080d` | Page background base |
| `surface-0` | `#0c0f17` | Lowest surface (inputs, code blocks) |
| `surface-1` | `#11141d` | Default card/panel |
| `surface-2` | `#161a25` | Hover / elevated panel |
| `surface-3` | `#1d2230` | Pressed / max elevation |
| `border` | `rgba(255,255,255,.06)` | Default 1px hairline |
| `border-strong` | `rgba(255,255,255,.10)` | Elevated/strong hairline |
| `text` | `#e8ebf2` | Primary text |
| `text-dim` | `#9aa3b7` | Secondary text |
| `text-mute` | `#5d6478` | Tertiary text / labels |
| `violet-{300..600}` | tailwind defaults | Primary accent ramp |
| `indigo-500` | `#6366f1` | Primary gradient mid-stop |
| `cyan-{300,400}` | tailwind defaults | Info / gradient end-stop |
| `emerald-400` | `#34d399` | Success / scope:read |
| `amber-400` | `#fbbf24` | Warning / scope:write |
| `rose-{400,500}` | tailwind defaults | Danger / scope:admin / revoked |

A `primary` gradient utility — `linear-gradient(135deg, #8b5cf6 0%, #6366f1 50%, #22d3ee 110%)` — is exposed as a Tailwind utility (`bg-primary-gradient`) and used on primary CTAs, the brand logo mark, and selective highlights. **Used sparingly** — not on every accent surface.

### Typography

- **UI:** Inter (400/500/600/700), self-hosted via `frontend/src/index.css` `@font-face` so it works offline; Thymeleaf templates use the same font via a CDN link in the existing CDN-based setup (acceptable tradeoff for the demo surface — it falls back to system UI fonts if blocked).
- **Mono:** JetBrains Mono (400/500/700) for JTIs, JWTs, client IDs, device codes.

Scale (Tailwind extends `fontSize`):

| Token | Size / line-height | Tracking | Use |
|---|---|---|---|
| `display` | 40 / 1.1 | -0.02em | Page hero on OAuth screens |
| `h1` | 26 / 1.2 | -0.015em | Admin page titles |
| `h2` | 20 / 1.3 | -0.01em | Section headings |
| `body` | 14 / 1.5 | 0 | Default body |
| `sm` | 13 / 1.5 | 0 | Compact body, table rows |
| `label` | 11 / 1.4 | 0.18em UPPERCASE | Section labels, stat labels |
| `mono-sm` | 13 / 1.6 | 0 | Identifiers, code |

### Spacing, radius, shadow

- **Spacing**: stay on Tailwind's default 4px scale. Reduce default padding density vs. current (e.g., cards `p-5` instead of `p-8`, page padding `py-6` instead of `py-8`).
- **Radius**: `sm: 6px`, default `10px`, `lg: 14px`, `xl: 20px`. The current `rounded-2xl` (16px) feels too "marketing-y" against dense data — pull radii in.
- **Shadow**: two named utilities — `shadow-soft` (1px inset highlight + 1–2px ambient) and `shadow-elev` (large soft drop + inset highlight). Plus `shadow-glow-primary` for primary CTAs.

## Component Primitives

All primitives live in `frontend/src/components/ui/` as JSX components. The Thymeleaf templates do not import JSX, so for the OAuth surface we expose the same look via **Tailwind `@layer components` classes** defined in `frontend/src/index.css` (which gets compiled to `frontend/dist/assets/index-*.css` and served by Spring as a static asset). The Thymeleaf templates then link this stylesheet instead of relying on the Tailwind CDN.

> **Thymeleaf integration shift:** moving the OAuth templates off `<script src="https://cdn.tailwindcss.com">` and onto the compiled stylesheet is part of this sub-project. It's a small but real change — Spring serves `frontend/dist/` (or equivalent) as static content; the templates link `/assets/aurora.css`. The exact wiring is captured in the implementation plan.

### Primitives shipped in v1

1. **Button** — variants: `primary` (gradient), `default` (surface-3), `ghost` (transparent), `danger` (rose gradient); sizes: `sm`, `md`. Both React component + `.btn`, `.btn-primary`, etc. CSS classes.
2. **Input** — text/email/password with optional left icon, right icon (used for show-password toggle, copy buttons). Same dual-form (component + class).
3. **Card** — `.card` (surface-1 with hairline border, small shadow) and `.card-elev` (surface-2, stronger border and elevation).
4. **Badge** — base `.badge`, plus semantic variants `.badge-read`, `.badge-write`, `.badge-admin`, `.badge-info`, `.badge-success`, `.badge-danger`. With optional leading dot.
5. **ScopeBadge** — specialization that takes a scope string and resolves to read/write/admin/info via a known mapping (e.g., `openid`/`profile`/`email` → read; `*.write`/`*.send` → write; `*.manage`/`admin` → admin). Mapping lives in a tiny shared JS module and a parallel Java enum so both surfaces classify scopes consistently. **Both must produce the same classification.**
6. **Toast** — small floating confirmation (used heavily by sub-project 3 for copy-to-clipboard). React component manages a single global queue via context.
7. **Skeleton** — shimmer-animated placeholder block; React component takes `width`, `height`, or accepts children layout. Also `.skeleton` class for templates if needed.
8. **EmptyState** — illustration slot (defaults to a subtle icon + gradient halo), title, body, optional CTA. React component only.
9. **Code/Mono** — `.mono` utility class + a `<Mono>` React component for inline mono runs (JTIs, client IDs).

### Admin shell pieces refactored (not new components, just re-styled)

- **Layout / Nav** ([Layout.jsx](frontend/src/components/Layout.jsx)) — refined nav bar with brand mark (gradient square + name), nav links with active-state pill, right-side meta (signing key id badge + logout). Same nav items, same routes.
- **StatCard** ([StatCard.jsx](frontend/src/components/StatCard.jsx)) — adds optional icon (top-right corner, soft gradient bg), optional delta sub-line. Existing `{label, value}` API still works.
- **DataTable** ([DataTable.jsx](frontend/src/components/DataTable.jsx)) — visual refresh only in this sub-project: hairline borders, label-styled headers, hover row tinting. **Search/filter/sort are sub-project 3.**

## Architecture

```
frontend/
  src/
    index.css                  # @tailwind + @font-face + @layer components (.btn, .card, .badge, .input, .mono, .skeleton, etc.)
    tailwind.config.js         # Color tokens, fontSize scale, shadow utilities, primary-gradient
    components/
      ui/
        Button.jsx
        Input.jsx
        Card.jsx
        Badge.jsx
        ScopeBadge.jsx
        Toast.jsx              # + ToastProvider, useToast hook
        Skeleton.jsx
        EmptyState.jsx
        Mono.jsx
        index.js               # barrel re-export
      Layout.jsx               # refactored to use ui/* + new shell styling
      StatCard.jsx             # refactored
      DataTable.jsx            # visually refactored only
    lib/
      scopes.js                # scope -> risk classification (mirrors Java side)

src/main/
  java/.../oauth/
    ScopeRisk.java             # enum READ/WRITE/ADMIN/INFO + classify(String scope)
  resources/
    templates/oauth/*.html     # link compiled Aurora stylesheet instead of CDN; use .btn/.card/.badge classes
    static/                    # serve compiled aurora.css here (or rely on existing static pipeline)
```

The React build produces a stylesheet that the Thymeleaf templates link. The exact serving path (e.g., `/assets/aurora.css`) is decided in the implementation plan based on existing static-asset wiring in `application.yml`.

## Scope classification (shared logic)

A common classifier exists in two places, kept in lockstep:

- **JS:** `frontend/src/lib/scopes.js` exports `classifyScope(scope: string): 'read' | 'write' | 'admin' | 'info'`.
- **Java:** `ScopeRisk.classify(String scope)` returning the same enum.

Rules (v1, deliberately small — extended in sub-project 2):
1. Standard OIDC scopes (`openid`, `profile`, `email`, `address`, `phone`) → `read`
2. `offline_access` → `info`
3. Ends with `.write`, `.send`, `.modify`, `.create`, `.delete` → `write`
4. Contains `admin`, `manage`, ends with `.manage` → `admin`
5. Default → `read`

Both implementations are covered by tests (JS Vitest if added; Java JUnit using the existing test infra).

## Error handling

This sub-project introduces no new error paths. Existing UI error states (e.g., `loginError` rendered on `login.html`) get re-styled with the new Badge/Card classes but keep their current behavior.

## Testing

- **Java:** unit test `ScopeRiskTest` covering each classification rule + a default case.
- **JS:** if Vitest is added (zero-cost dev dep), a parity test mirroring the same cases in `scopes.test.js`. If we choose to skip Vitest in this sub-project, the classification is small enough to be visually verified against the Java side and revisited in sub-project 2.
- **Visual smoke:** start the Spring app + Vite dev server, hit Dashboard, Clients, Tokens, Inspector, Login, Consent, Device — confirm rendering. Computer-use / Playwright not required for this sub-project.

## Risks / open decisions

- **CDN-Tailwind → compiled stylesheet for Thymeleaf** is the most invasive structural change in this sub-project. If the existing static-asset pipeline doesn't already include the React build output on the classpath, we need to wire that up. Implementation plan must investigate Spring static config + `manifest.yml` / `Dockerfile` before committing.
- **Font hosting**: Inter/JetBrains Mono via Google Fonts CDN is the simplest start. If CSP or offline-demo concerns surface, self-host. Default to CDN for v1.
- **Backwards compatibility:** all existing component APIs (`<StatCard label value>`, `<DataTable columns data actions>`) are preserved. Pages don't need to change to keep working — they just get prettier.

## Definition of done

- `tailwind.config.js` exposes the full Aurora token set (colors, type scale, shadows, primary gradient).
- `frontend/src/index.css` defines the `@layer components` classes for all primitives, font imports.
- All 9 primitives exist under `frontend/src/components/ui/` with a barrel export.
- `Layout`, `StatCard`, `DataTable` rebuilt against the new tokens/primitives; existing pages render correctly without changes.
- All 6 OAuth templates link the compiled Aurora stylesheet and use the new classes; current OAuth flows still work end-to-end.
- `ScopeRisk` enum + classifier in Java, mirrored by `scopes.js` in JS; Java unit tests pass.
- `npm run build` succeeds; `./mvnw clean package` (or equivalent) succeeds; both surfaces render correctly in a smoke test.
