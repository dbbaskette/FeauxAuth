# OAuth Flow UX Overhaul — Sub-project 2

**Date:** 2026-05-13
**Builds on:** Aurora design system (sub-project 1)

## Goals

Bundle three UX wins for the server-rendered OAuth surface, now that they share the Aurora visual language:

1. **#1 — Split the device flow** into three steps (confirm code → sign in → approve), matching the Google/GitHub/Netflix pattern.
2. **#2 — Richer consent screen** with a client avatar, redirect-URI hostname trust signal, and risk-coded scope rows (already wired by sub-project 1; this sub-project tightens the layout and applies the same treatment to the device-flow consent).
3. **#7 — Friendlier OAuth error page** with a plain-English hint and "likely cause" mapped from the OAuth error code.

## #1 — Device flow split

Three pages instead of one:

| Step | Path (GET) | Template | POST handler | Stores in session |
|---|---|---|---|---|
| 1 | `/device` | `oauth/device-code.html` | `/device` (existing path) | `device_user_code` after validation |
| 2 | `/device/sign-in` | `oauth/device-signin.html` | `/device/sign-in` | `device_user_id` after auth |
| 3 | `/device/authorize` | `oauth/device-consent.html` | `/device/authorize` | — (final approve/deny) |

Backwards-compat: the old single-page POST `/device` (with email + password in the body) keeps working — it's the path the CLI samples use, and breaking it is not in scope. When the form posts with no email/password, the handler treats it as step-1 (code verification) and redirects to `/device/sign-in`. Step 2 and 3 are new endpoints; they're additive.

Each page uses the Aurora card-elev surface, the new brand-mark, and progress hints ("Step 2 of 3"). The user can cancel at any step, which marks the code denied — same behavior as today.

## #2 — Richer consent (both flows)

`consent.html` and the new `device-consent.html` share a layout fragment `oauth/fragments/consent-card.html` that takes:

- `clientName`, `clientInitial`
- `redirectHost` (just the host portion of `redirect_uri`, for the authorization-code flow only)
- `userEmail`
- `scopeItems` (the existing `List<ScopeItem>` records — already in place from sub-project 1)
- `approveAction`, `denyAction` (form action URLs; differ between the two flows)
- `signedInAs` (boolean — show the "Signed in as …" footer)

Visually: the client avatar gets the first letter on a gradient ring; the redirect host is shown directly under the title as `Returns to <code>{host}</code>`; scope rows are full-width with the risk badge on the left, scope name + description on the right.

`AuthorizeController.authorize()` adds `redirectHost = URI.create(redirectUri).getHost()` to the model. Null-safe — if parse fails, omit the host line.

## #7 — Friendlier errors

`error.html` already shows the error code and description after sub-project 1. Add a `hint` line beneath the description, populated from a small map of well-known OAuth error codes:

```
invalid_request       → "The request is missing a required parameter or contains an invalid value."
invalid_client        → "The client_id is unknown or disabled. Check the client registration in the admin console."
invalid_grant         → "The authorization code, refresh token, or device code is expired, revoked, or already used."
unauthorized_client   → "The client is not allowed to use this grant type."
unsupported_response_type → "Only response_type=code is supported."
unsupported_grant_type    → "This grant type isn't enabled. Check the token endpoint docs."
invalid_scope         → "One of the requested scopes is not allowed for this client. Check allowed_scopes in the admin console."
access_denied         → "The user declined the authorization request."
expired_token / expired_code → "The code has expired. Return to the device and request a new one."
```

The hint lives in a new `OAuthErrorHints` utility class (Java) and is added to the model wherever `oauth/error` is returned. Existing callers don't have to be changed if a helper method `model.addAttribute("errorHint", OAuthErrorHints.hint(errorCode))` is used. Add a small helper to make this one-line.

## Testing

- **Java:** unit test for `OAuthErrorHints.hint()` covering the 9 known codes + default fallback.
- **Java:** controller test for the device split flow happy path — POST step 1 → 302 to `/device/sign-in`, POST step 2 → 302 to `/device/authorize`, POST step 3 with approve=true → 302 to device-success.
- **Manual smoke:** run the jar, walk through the device flow with a seeded user; confirm the existing single-page POST still works.

## Definition of done

- Three device templates (`device-code.html`, `device-signin.html`, `device-consent.html`) render in Aurora and complete the flow end-to-end.
- `consent-card.html` fragment exists; both consent surfaces use it.
- `redirectHost` shown on the authorization-code consent.
- `OAuthErrorHints` utility exists; `error.html` shows the hint when one is available.
- Tests pass; manual smoke ok.
