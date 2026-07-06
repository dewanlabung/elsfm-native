# The `common/foundation` Package

BeMusic (ELSFM) is split into two layers. Understanding this split is the single
most important thing for working in the backend: **most logic does not live in
`app/`** — it lives in the shared `common/foundation` package, and `app/` is a
thin, product-specific layer on top.

## The split

```
common/foundation/        # Shared framework package (the bulk of the code)
  src/
    Auth/BaseUser.php          # auth, permissions, billing, follows, sessions
    Channels/BaseChannel.php   # polymorphic content channels
    Core/BaseController.php    # success()/error() envelope, authorize(), SEO
    Core/BaseModel.php         # base Eloquent model
    Core/BaseFormRequest.php   # base form request
    Core/Policies/BasePolicy.php  # permission + quota authorization helpers
    Database/Datasource/       # filtering + pagination (replaces API Resources)
  database/migrations/         # core schema (users, follows, roles, files, ...)

app/                      # ELSFM-specific layer (small)
  Models/User.php              # extends Common\Auth\BaseUser
  Models/Channel.php           # extends Common\Channels\BaseChannel
  Models/BackstageRequest.php  # extends Common\Core\BaseModel
  Http/Controllers/*           # extend Common\Core\BaseController
  Http/Requests/*              # extend Common\Core\BaseFormRequest
  Policies/*                   # extend Common\Core\Policies\BasePolicy
  Services/<Domain>/*          # invokable execute() actions
```

Rule of thumb: if behavior is generic (auth, permissions, billing, files,
pagination, channels), it's in `common/foundation`. If it's music/ELSFM-specific
(backstage requests, artist profiles, playlists), it's in `app/`.

## Extending the base classes

### Models — extend a base, add only what's new

`BaseUser` already provides roles, permissions, billing, file entries, sessions,
and the `follows` relations (`followedUsers()`, `followers()`). The `app/` model
adds only music relations.

```php
class User extends \Common\Auth\BaseUser
{
    const MODEL_TYPE = 'user';

    public function playlists(): BelongsToMany
    {
        return $this->belongsToMany(Playlist::class);
    }
}
```

Do not redeclare inherited relations. Override `toArray()`, `filterableFields()`,
or `toSearchableArray()` only when you need behavior different from the base.

### Controllers — extend `BaseController`

Gives you `success()` / `error()` (the standard JSON envelope), a guest-aware
`authorize()`, demo-site blocking (`blockOnDemoSite()`), and SEO/client rendering.

```php
use Common\Core\BaseController;

class BackstageRequestController extends BaseController
{
    public function index(): Response
    {
        $this->authorize('index', [BackstageRequest::class, $userId]);
        $pagination = (new Datasource($builder, $this->request->all()))->paginate();
        return $this->success(['pagination' => $pagination]);
    }
}
```

`success()` auto-adds `"status": "success"`, calls `toArray()` on any `Arrayable`
values, and (for frontend requests) can inject SEO tags. `error($message, $errors,
$status)` returns the matching error shape.

### Form Requests — extend `BaseFormRequest`

`BaseFormRequest::authorize()` returns `true` by default (authorization is handled
by policies in the controller via `$this->authorize(...)`, not in the request).
You typically override only `rules()`.

```php
use Common\Core\BaseFormRequest;

class CrupdateBackstageRequestRequest extends BaseFormRequest
{
    public function rules(): array
    {
        return [
            'artist_name' => ['required', 'string', 'min:3'],
            'data' => 'required|array',
        ];
    }
}
```

### Policies — extend `BasePolicy`

`BasePolicy` is constructor-injected with `Request` and `Settings` and provides:

- `hasPermission($user, 'foo.create')` — permission check that also handles the
  guest role.
- `authorizePermission($user, $permission)` — returns a permission-aware response.
- `storeWithCountRestriction($user, Model::class)` — enforces plan-based quota
  limits (e.g. max playlists) and returns an upgrade action when exceeded.
- `denyWithAction()` / `upgradeAction()` — deny responses that carry a CTA.

```php
use Common\Core\Policies\BasePolicy;

class BackstageRequestPolicy extends BasePolicy
{
    public function store(User $user)
    {
        return $this->hasPermission($user, 'backstageRequests.create');
    }

    public function update(User $user, BackstageRequest $request)
    {
        return $this->hasPermission($user, 'backstageRequests.update')
            || $request->user_id === $user->id;
    }
}
```

## Practical implications for the Flutter client

- The API envelope is always `{ "status": "success", ... }` or
  `{ "message": ..., "errors": {...} }` (from `BaseController`), not a bare
  Laravel resource. Parse accordingly.
- List endpoints return a `pagination` object produced by `Datasource`
  (see datasource-pagination.md), with `page` / `perPage` / `query` / `order`
  query params — there are no API Resource wrappers to anticipate.
- Polymorphic lists (channel content, search) return mixed items discriminated by
  `model_type` (`user`, `playlist`, `album`, `track`, `channel`). Switch on
  `model_type` when decoding into Dart models.

## Gotcha: don't look for music models in `app/`

`Track`, `Album`, `Artist`, and `Playlist` are referenced from this repo but their
class definitions live in the BeMusic package, not in `app/Models/` of this script.
`app/Models/` here contains only `User`, `Channel`, `BackstageRequest`, `Tag`,
`ProfileLink`, `ProfileImage`, `ProfileDetails`. To add behavior to a music model,
follow this same extend-the-base pattern in the package layer.
