# ELSFM Native (Kotlin/Android) — Phase 1: Foundation Design

## Context

`elsfm_flutter` (at `/Users/siku/Documents/GitHub/elsfm_flutter`) is an existing Flutter client for
elsfm.com that has broken playback, unsynced login/session, unsynced play history, and non-working
saved-password support. Rather than continue patching the Flutter client, this project is a **new,
separate, native Android (Kotlin) app** for elsfm.com, built from scratch, that does not share or
merge code with `elsfm_flutter`.

Reference architecture: `/Users/siku/Documents/GitHub/ymusic` — a mature, native Android Kotlin
music app (Jetpack Compose, Media3/ExoPlayer, Room, Ktor, Coil, multi-module Gradle). We borrow its
proven module layout and background-playback approach, not its data source (ymusic talks to
YouTube's innertube API; we talk to the bemusic/elsfm Laravel REST API).

Backend: `/Users/siku/Documents/GitHub/bemusic script` — a Laravel app built on the "Common"
vebto SaaS foundation (shared across bemusic/betube-style products), using Laravel Sanctum for auth
and a generic Channel/ChannelItem content model (channels nest tracks, playlists, albums, artists).

## Verified ground truth (not assumptions)

Confirmed by reading `common/foundation/src/Auth/Controllers/MobileAuthController.php`,
`common/foundation/src/Core/Bootstrap/MobileBootstrapData.php`,
`common/foundation/src/Auth/Middleware/VerifyApiAccessMiddleware.php`, and by inspecting the live
`elsfm.com` site's already-authenticated session (`window.bootstrapData`) and network traffic in
Chrome — no credentials were entered by the assistant at any point; the browser session was already
authenticated.

- **Mobile auth is a dedicated endpoint**, separate from the web SPA's cookie-session auth:
  `POST /api/v1/auth/login` with body `{ email, password, token_name }` (Fortify username field,
  usually email). Returns a Sanctum personal access token as `access_token`, plus `user`
  (profile, roles, permissions), `settings`, `locales`, `themes`, `menus` in one payload
  (`MobileBootstrapData::init()->refreshToken($token_name)`).
- `MobileBootstrapData` has a `transformValuesForFlutter()` step — the backend was already built
  with a mobile client's needs in mind (RGBA color arrays, px-based radii instead of CSS strings).
- Registration: `POST /api/v1/auth/register`. Password reset: `POST /api/v1/auth/password/email`.
  Social login: `GET /api/v1/auth/social/{provider}/callback`.
- Every `v1` API route runs through `VerifyApiAccessMiddleware`: requests must either come from the
  registered frontend origin (Sanctum SPA "stateful" check) or the acting user must hold the
  `api.access` permission. **Risk to verify in Phase 1 implementation**: confirm the default user
  role actually has `api.access`, or native login will 401 despite correct credentials.
- A `single_device_login` setting exists (`Auth::logoutOtherDevices()` on login if enabled) — if
  enabled server-side, logging in from the native app could invalidate the web session's password
  hash check on other devices. **Risk to verify** before shipping login (read `settings` on the
  live site, or ask the backend owner).
- Real permission set observed on the test account: `music.view/play/download/offline/embed`,
  `playlists.view/create`, `comments.*`, `backstageRequests.create`. The native app's session model
  must carry these through so playback/download gating matches the web app's behavior.
- Content is modeled as `Channel` / `ChannelItem` (a generic nestable CMS entity), not flat
  `Track`/`Album`/`Artist` tables — elsfm.com itself is one channel. Full schema exploration is
  deferred to the Phase 3 (Library/Playlists) spec; Phase 1 only needs the `User`/`AuthSession`
  shape.

## Overall phased roadmap (for context — only Phase 1 is in scope for this spec)

1. **Foundation** (this spec): project scaffold, networking client, synced auth (login, saved
   password, session persistence, logout), Room DB setup.
2. **Player**: Media3/ExoPlayer background playback service, player UI, play-history sync — the two
   things most visibly broken in the Flutter app today.
3. **Library/Playlists**: browse, search, artist/album pages, playlist CRUD, following.
4. **Rest**: downloads/offline, recommendations, lyrics, notifications, profile/settings polish.

Each phase gets its own design spec, implementation plan, and review before the next phase starts.

## Phase 1 scope

### Module architecture (Gradle, modular — "Now in Android" style, matching ymusic's own split)

```
elsfm-native/
├── app/                    # Application class, Hilt DI graph root, nav host
├── core/
│   ├── model/               # Plain Kotlin data classes: User, AuthSession, ApiError — no Android deps
│   ├── network/              # Ktor HttpClient, AuthPlugin (Bearer token injection + 401 handling), ApiResult<T>
│   ├── database/             # Room: AppDatabase, UserDao, UserEntity (profile cache only — token is NOT stored here)
│   ├── designsystem/          # Compose theme/tokens (color, type, spacing) for elsfm branding
│   └── common/               # Result/dispatcher helpers, base ViewModel scaffolding
└── feature/
    └── auth/                  # LoginScreen (Compose), LoginViewModel, AuthRepository, SessionManager
```

Later phases add `core:media` + `feature:player` (Phase 2), `feature:library` / `feature:playlists`
/ `feature:search` / `feature:artist` / `feature:album` (Phase 3), and
`feature:downloads` / `feature:recommendations` / `feature:lyrics` / `feature:notifications` /
`feature:profile` (Phase 4) — new modules only, no restructuring of Phase 1's modules.

### Tech choices

- **UI**: Jetpack Compose, Material 3.
- **Networking**: Ktor client (OkHttp engine on Android), `kotlinx.serialization` for JSON — matches
  the project's existing Kotlin testing rules (Ktor `MockEngine`) and `ymusic`'s own stack.
- **DI**: Hilt (Android-only app, no KMP target needed).
- **Local storage**: Room for cached profile data; `EncryptedSharedPreferences` (AES-256-GCM) for
  the Sanctum access token — the token never touches Room or plain `SharedPreferences`.
- **Saved password**: `androidx.credentials` Credential Manager API, so the OS-level
  save/autofill-password (and future passkey) prompt fires after a successful login. This is the
  standard native mechanism Flutter's plugin bridge tends to miss and is the most likely root cause
  of "saved password didn't work" in the existing app.

### Auth / session data flow

1. **App start**: `SessionManager` reads the token from `EncryptedSharedPreferences`. If present,
   it's attached to a lightweight authenticated request (e.g. fetch current user) to confirm it's
   still valid; on success, go to the (Phase 2+ placeholder) home screen, on 401 clear session and
   show Login.
2. **Login**: `LoginViewModel` collects email/password (optionally pre-filled by Credential Manager
   autofill) → `AuthRepository.login()` → Ktor `POST /api/v1/auth/login` with
   `token_name = "android-${installationUuid}"` (a stable per-install UUID, so each device gets its
   own revocable Sanctum token and multi-device behavior is visible/debuggable) → on success, store
   `access_token` in `EncryptedSharedPreferences`, cache `user` profile in Room, and trigger the
   Credential Manager save-password prompt.
3. **Authenticated requests**: a Ktor `AuthPlugin` reads the current token from `SessionManager` and
   sets the `Authorization: Bearer <token>` header on every request automatically — no per-call
   wiring, no scattered "am I logged in" checks in feature code.
4. **Session death**: the same `AuthPlugin` centrally intercepts `401` responses, clears the stored
   session, and emits a single "force logout" event that the nav host observes to route back to
   Login — this is the fix for the class of bug where the app silently holds a dead session
   ("login didn't sync").
5. **Logout**: clears the token from `EncryptedSharedPreferences`, clears the cached `UserEntity`
   from Room, and (optionally, later) calls `DELETE /api/v1/access-tokens/{tokenId}` to revoke
   server-side.

### Error handling

The network layer never throws into UI code. All calls return a sealed `ApiResult<T>`:
`Success<T>`, `ValidationError(fields: Map<String, List<String>>)` (422 responses map directly to
form field errors), `Unauthorized`, `NetworkError(cause)`. `LoginViewModel` maps these to UI state
directly — no try/catch in Compose screens.

### Testing

- Ktor `MockEngine` tests for the auth API client: login success, wrong password, 422 validation,
  expired/invalid token → 401.
- Fake `AuthRepository` + Turbine tests for `LoginViewModel` state transitions (idle → loading →
  success/error).
- Room in-memory database test for `UserDao`.
- No real network credentials are used in any automated test — all backend interaction in tests is
  mocked. The one real end-to-end login check against production elsfm.com is a manual step the
  project owner performs themselves on the emulator/device (the assistant does not type passwords
  into forms or commands, by policy, regardless of authorization).

### Out of scope for Phase 1

Playback (Media3), play history recording, library/search/playlists, downloads, recommendations,
lyrics, notifications, profile editing — all deferred to their respective phases per the roadmap
above.

## Environment / tooling notes

- Android SDK already present at `~/Library/Android/sdk` with an existing AVD (`Pixel_10_Pro`).
  `adb`/`emulator`/`avdmanager` exist on disk but aren't on `PATH` — invoke by full path or export
  `ANDROID_HOME` per Bash session. Android Studio's GUI is not required to build, test, or run the
  app; it's only needed if the project owner wants to interact with the emulator UI directly or use
  Android Studio's visual tooling.
- GitHub: repo will be `dewanlabung/elsfm-native` (private), created and pushed once Phase 1 code
  exists and compiles/tests pass locally.

## Risks carried into implementation

1. Confirm the default user role has `api.access` permission (or native login 401s despite correct
   credentials) — check via the live account's role/permission data or a manual login test.
2. Confirm whether `single_device_login` is enabled on elsfm.com's settings and, if so, decide
   whether native login should warn the user that it will sign other devices out.
