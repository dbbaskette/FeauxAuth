# Admin SPA UX Polish — Sub-project 3

**Date:** 2026-05-13
**Builds on:** Aurora design system (sub-project 1)

## Goals

Bundle the four remaining UX wins on the React admin SPA, all of which lean on Aurora primitives that already exist (Skeleton, EmptyState, Toast, Badge, Mono):

1. **#3 — Loading skeletons + richer empty states** across all list pages.
2. **#4 — Search / filter / sort** on the shared `DataTable`.
3. **#5 — Copy-to-clipboard** for every visible identifier (JTIs, client IDs, user IDs, secrets).
4. **#6 — Token row → Inspector deep-link**: clicking a token jumps to the Inspector pre-loaded with that token's metadata.

## #3 — Skeletons & empty states

`DataTable` gains a `loading` prop. When `loading={true}` and there's no data yet, it renders a configurable number of skeleton rows (default 5) instead of the empty state. Each list page (`Clients`, `Users`, `Tokens`) tracks an `isLoading` state initialized to `true`, set to `false` after the initial fetch resolves (success or error).

Empty-state copy is already in place from sub-project 1 (`emptyTitle`, `emptyDescription`); we just keep it visible *only* when loading is false.

## #4 — Search / filter / sort

`DataTable` gains three opt-in features driven by a new `<TableToolbar>` component rendered above the table:

- **Search:** prop `searchableKeys: string[]` — when present, the toolbar shows a search input that filters rows whose values at any of those keys (case-insensitive substring match) include the term.
- **Filter:** prop `filters: { key, label, options: [{value, label}] }[]` — each entry renders a dropdown; rows where `row[key] === selectedValue` (or `selectedValue === '_all'`) pass.
- **Sort:** column entries can declare `sortable: true`. Clicking the header toggles asc/desc and sorts the displayed rows. Default sort can be specified via `defaultSort: { key, dir }`.

All three operate on the `data` prop in-place (no API changes); they're presentation-only. Pages that opt in:

- **Tokens:** filters = `[{key: 'revoked', label: 'Status', options: [All, Active, Revoked]}]`; searchable = `['jti', 'clientId', 'userId', 'scope']`; sortable = `createdAt`, `expiresAt`.
- **Users:** searchable = `['email', 'displayName']`; sortable = `email`, `lastLoginAt`.
- **Clients:** searchable = `['clientId', 'name']`; sortable = `name`, `accessTokenTtl`.

Pagination already exists on Tokens — search/filter applies to the *current page only* (a fair tradeoff for v1; we can wire server-side later).

## #5 — Copy-to-clipboard everywhere

A new `<CopyableMono>` primitive (under `frontend/src/components/ui/`) wraps `<Mono>` with a small inline copy button. Clicking copies the value to the clipboard and fires a toast (`useToast().success(...)`). Falls back to plain `<Mono>` when `navigator.clipboard` isn't available.

Used in:
- **Clients table** — `clientId` column
- **Tokens / Dashboard table** — `jti` column (full JTI on click — no truncation when copying)
- **Users table** — id (if shown)
- **ClientForm secret reveal screen** — already uses a Copy Secret button; switch to `<CopyableMono>` for consistency
- **Inspector page** — copy buttons on the decoded header / payload JSON blocks

The truncation logic in `tokenColumns.js` keeps the visual `xxxxx…` but the *full* value is what gets copied — this means `<CopyableMono>` accepts an optional `displayValue` prop separate from `children` (the value).

## #6 — Token row → Inspector deep-link

The admin server doesn't store the full JWT (only its claims metadata), so we can't hand the Inspector a real JWT to verify. Instead, the Inspector gains a **metadata mode**:

- New endpoint **`GET /api/admin/tokens/{jti}`** returning the stored `AccessToken` (404 if missing).
- The Inspector React page detects the `?jti=…` query param. If present, it fetches the metadata and renders a "Token Metadata" view: JTI, client, user, scopes (with risk badges), issued/expires timestamps, and revocation status. A small notice explains "Signature and full payload aren't available in metadata mode — paste the JWT below for full validation."
- The paste textarea is still present below the metadata view, so the user can paste the original JWT to upgrade to full inspection.
- **Tokens table** — the JTI column becomes a link (`<Link to={'/inspector?jti=' + row.jti}>`), styled like a normal row value but with a hover underline.

## Testing

- **Manual smoke:** load Clients/Users/Tokens pages, confirm skeletons appear briefly then resolve. Type in the search box → table filters. Click a JTI → lands on Inspector with metadata. Click copy buttons → toast confirms.
- **No new unit tests** for this sub-project — the logic is presentational and purely reactive to user input. Existing controller tests cover the new `GET /api/admin/tokens/{jti}` endpoint indirectly via integration paths.

## Definition of done

- `DataTable` supports `loading`, search, filter, sort.
- `<CopyableMono>` exists and is used in the four named places.
- `GET /api/admin/tokens/{jti}` returns the stored metadata.
- Inspector page handles `?jti=` query param and renders the metadata view.
- Frontend builds; jar packages; manual smoke ok.
