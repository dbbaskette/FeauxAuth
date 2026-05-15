# Aurora Design System — Implementation Plan

**Spec:** [2026-05-13-aurora-design-system-design.md](../specs/2026-05-13-aurora-design-system-design.md)

**Goal:** Land the Aurora visual identity across both surfaces (React admin SPA and Thymeleaf OAuth templates) plus a tiny shared scope-risk classifier. Pure re-skin + primitives — no feature changes.

**Approach for the cross-surface stylesheet:** Keep the existing CDN-Tailwind pipeline for Thymeleaf templates, but extract the Aurora config (tokens, `@layer components`, font imports) into a single Thymeleaf fragment (`oauth/fragments/aurora-head.html`) that every template includes. The React SPA gets the same tokens via `tailwind.config.js` + `frontend/src/index.css`. Tokens are defined once **conceptually** but expressed in two places — the fragment for templates, the Tailwind config for the SPA. A short comment in each file points to the spec as the source of truth.

---

## Phase 1 — Tailwind tokens & global CSS (SPA)

- [ ] **1.1** Update `frontend/tailwind.config.js` to extend the theme with Aurora tokens: `colors` (bg, surface-0..3, border, border-strong, text, text-dim, text-mute), `fontFamily` (sans → Inter, mono → JetBrains Mono), `fontSize` (display, h1, h2, body, sm, label, mono-sm with line-heights & tracking), `borderRadius` (sm/DEFAULT/lg/xl as 6/10/14/20), `boxShadow` (soft, elev, glow-primary), `backgroundImage` (primary-gradient, primary-gradient-soft).
- [ ] **1.2** Rewrite `frontend/src/index.css` to import Inter + JetBrains Mono from Google Fonts (`@import url(...)` at the top), then `@tailwind base/components/utilities`, then `@layer base` with body background gradient + base typography, then `@layer components` with: `.btn` (+ `.btn-primary`, `.btn-ghost`, `.btn-danger`, `.btn-sm`), `.input`, `.field-label`, `.card` (+ `.card-elev`), `.badge` (+ `.badge-read`, `.badge-write`, `.badge-admin`, `.badge-info`, `.badge-success`, `.badge-danger`), `.skeleton` (with shimmer keyframes), `.mono`, `.stat-label`, `.eyebrow`. Mirror the visual treatment shown in the Aurora direction mockup.
- [ ] **1.3 — Verify:** `cd frontend && npm install && npm run build` succeeds; produces `src/main/resources/static/admin/assets/index-*.css` containing the new utilities.

## Phase 2 — Shared scope-risk classifier

- [ ] **2.1** Create `frontend/src/lib/scopes.js` exporting `classifyScope(scope: string): 'read' | 'write' | 'admin' | 'info'` and `SCOPE_RISK_LABELS` map. Rules per spec: OIDC standards → read; `offline_access` → info; suffixes `.write/.send/.modify/.create/.delete` → write; contains `admin`, `manage`, `.manage` → admin; default → read.
- [ ] **2.2** Create `src/main/java/com/baskettecase/feauxauth/oauth/ScopeRisk.java` — `enum ScopeRisk { READ, WRITE, ADMIN, INFO }` with `static ScopeRisk classify(String scope)` mirroring the JS rules exactly. Add `static List<ScopedRisk> classifyAll(Collection<String>)` helper returning a list of `ScopedRisk(scope, risk)` records — used by sub-project 2 consent rendering.
- [ ] **2.3** Create `src/test/java/com/baskettecase/feauxauth/oauth/ScopeRiskTest.java` covering each rule + default fallback + null/empty handling.
- [ ] **2.4 — Verify:** `./mvnw -pl . test -Dtest=ScopeRiskTest` passes (or run the full suite if scoped run isn't trivial here).

## Phase 3 — Component primitives (SPA)

- [ ] **3.1** Create `frontend/src/components/ui/Button.jsx` — props: `variant` (primary/default/ghost/danger, default primary), `size` (sm/md, default md), `as` (default 'button'), passes through className + children. Composes the `.btn` + `.btn-*` classes.
- [ ] **3.2** Create `frontend/src/components/ui/Input.jsx` — props: `label`, `type`, `iconLeft`, `iconRight`, `value`, `onChange`, plus pass-through. Renders `.field-label` + `.input-group` wrapper when icons present, else bare `.input`. Auto-includes show/hide toggle when `type="password"`.
- [ ] **3.3** Create `frontend/src/components/ui/Card.jsx` — exports `<Card elev?>` rendering `.card` or `.card-elev`. Optional `header`, `footer` slots.
- [ ] **3.4** Create `frontend/src/components/ui/Badge.jsx` — props: `variant` (default/read/write/admin/info/success/danger), `dot` (default true for semantic variants). Renders `.badge .badge-*`.
- [ ] **3.5** Create `frontend/src/components/ui/ScopeBadge.jsx` — props: `scope`. Imports `classifyScope` from `lib/scopes.js` and renders `<Badge variant={risk}>` with the scope name. Used by sub-project 2.
- [ ] **3.6** Create `frontend/src/components/ui/Toast.jsx` — defines `ToastProvider` (context + queue), `useToast()` hook (`toast.show('Copied')`, `toast.error(...)`), and `<ToastViewport>` placed once in the app shell. Toast auto-dismisses after 2.5s, max 3 stacked. Used by sub-project 3.
- [ ] **3.7** Create `frontend/src/components/ui/Skeleton.jsx` — props: `width`, `height`, `className`. Renders a div with `.skeleton` and inline styles.
- [ ] **3.8** Create `frontend/src/components/ui/EmptyState.jsx` — props: `icon` (defaults to a subtle gradient circle), `title`, `description`, `action` (optional ReactNode). Used by sub-project 3.
- [ ] **3.9** Create `frontend/src/components/ui/Mono.jsx` — tiny wrapper rendering `<span className="mono">{children}</span>`. Cheap convenience; encourages consistent treatment of identifiers.
- [ ] **3.10** Create `frontend/src/components/ui/index.js` — barrel re-export of every primitive.

## Phase 4 — Refactor admin shell

- [ ] **4.1** Refactor `frontend/src/App.jsx` to wrap routes in `<ToastProvider>` and place `<ToastViewport>` so toasts work everywhere.
- [ ] **4.2** Refactor `frontend/src/components/Layout.jsx`: replace the gray-on-gray nav with the Aurora shell — gradient logo mark + brand, nav links with `.nav-link`/`.nav-link.active` styling (define classes in `index.css`), right-side meta area (placeholder for signing key id badge — pulled from `/api/admin/dashboard/stats` is a sub-project-3 polish; for now just keep the Logout button styled as `<Button variant="ghost" size="sm">`).
- [ ] **4.3** Refactor `frontend/src/components/StatCard.jsx`: accept optional `icon` (ReactNode) and `delta` (string or `{text, tone: 'positive'|'neutral'|'warning'}`). Render with new `.card` styling, label using `.stat-label`, value at 32px/700/-0.02em. Existing callers (`<StatCard label value>`) still render correctly.
- [ ] **4.4** Refactor `frontend/src/components/DataTable.jsx` visually only: header → label-style uppercase tracked text; rows → 13.5px text-dim; hover → surface-2 tint; hairline dividers; empty-state row uses `<EmptyState>` (small variant) instead of plain "No data" text.
- [ ] **4.5** Pages (`Dashboard`, `Clients`, `Users`, `Tokens`, `Inspector`, `Login`, `ClientForm`, `UserForm`): grep for direct uses of `bg-gray-800`, `bg-gray-700`, `rounded-2xl`, `text-white`, etc.; replace with the new tokens (`bg-surface-1`, `rounded-lg`, `text-text`) and standard primitives where they fit (e.g., raw `<button class="bg-indigo-600...">` becomes `<Button variant="primary">`). Keep behavior identical.
- [ ] **4.6 — Verify:** `npm run dev` starts; visit `/admin/`, log in (with backend running), confirm Dashboard, Clients, Users, Tokens, Inspector all render with Aurora styling and remain functional.

## Phase 5 — Thymeleaf Aurora head fragment

- [ ] **5.1** Create `src/main/resources/templates/oauth/fragments/aurora-head.html` containing: `<th:block th:fragment="head">` wrapping `<meta>` viewport, the Inter+JetBrains Mono Google Fonts `<link>`, the Tailwind CDN script, an inline `<script>tailwind.config = { theme: { extend: { /* Aurora tokens, mirrored from frontend/tailwind.config.js */ } } }</script>`, and a `<style type="text/tailwindcss">` block with the Aurora `@layer components` classes (subset needed for OAuth screens: `.btn`, `.input`, `.card`, `.badge`, `.scope`). Plus a `<style>` block for the gradient body background and font defaults.
- [ ] **5.2** Update `templates/oauth/login.html` to: (a) `<head>` includes `<th:replace="oauth/fragments/aurora-head :: head"></th:replace>`, dropping the local CDN script + meta; (b) body uses the new gradient bg classes; (c) container uses `.card-elev`; (d) inputs use `.input`; (e) primary button uses `.btn .btn-primary`; (f) error banner uses `.badge`/`.card` styling. Same form fields, same POST.
- [ ] **5.3** Update `templates/oauth/consent.html` similarly: brand-stamped header with a gradient client avatar (single letter from `clientName`), redirect-URI hostname shown below the title (`th:text="${redirectHost}"` — add `redirectHost` to the model in `AuthorizeController` consent view), and scope rows rendered with risk badges. **Note:** this template's full UX redesign (split scope list, "first time" badge, etc.) lands in sub-project 2; here we only re-skin and add risk badges so the data path is in place.
- [ ] **5.4** Update `templates/oauth/device.html`, `device-success.html`, `logout.html`, `error.html` to use the fragment + new classes. Same forms, same behavior. (Full device-flow split is sub-project 2.)
- [ ] **5.5** Wire `redirectHost` model attribute in `AuthorizeController` (or wherever consent.html is rendered) — extract `URI.create(client.redirectUri).getHost()`; null-safe fallback to the raw URI. If consent uses multiple controllers, do it in each.
- [ ] **5.6 — Verify:** Run `./mvnw spring-boot:run`; navigate through a manual OAuth flow (use one of the seeded test clients) — login → consent → token — confirm each screen renders in Aurora and the flow still completes.

## Phase 6 — Final verification

- [ ] **6.1** `./mvnw clean package` succeeds end-to-end (frontend build + Java compile + tests).
- [ ] **6.2** Run the packaged jar: `java -jar target/feauxauth-1.0.0.jar`; open `http://localhost:8080/admin/` and confirm admin shell loads with Aurora styling. Log in, visit each tab.
- [ ] **6.3** From a fresh browser, hit a seeded OAuth client's authorize URL; confirm `login.html`, `consent.html` render in Aurora and the flow yields a token successfully.
- [ ] **6.4** Smoke-test device flow: `POST /oauth/device_authorization` with a public client; visit `/device` with the returned `user_code`; confirm `device.html` and `device-success.html` render.
- [ ] **6.5** Commit phase-by-phase as you go (one commit per phase is fine).
