# Service Pattern (invokable `execute()`)

BeMusic services are **single-action classes with one public `execute()` method**,
not multi-method "manager" services. Each unit of business logic is its own small
class, resolved from the container and invoked once. This is the action/command
pattern, not the textbook "one service per entity with many methods".

## Shape

```php
// app/Services/Backstage/CrupdateBackstageRequest.php
namespace App\Services\Backstage;

class CrupdateBackstageRequest
{
    public function __construct(protected BackstageRequest $backstageRequest) {}

    public function execute(
        array $data,
        BackstageRequest $backstageRequest = null,
    ): ?BackstageRequest {
        if (!$backstageRequest) {
            $backstageRequest = $this->backstageRequest->newInstance([
                'user_id' => Auth::id(),
            ]);
        }
        // ...build attributes...
        $backstageRequest->fill($attributes)->save();
        return $backstageRequest;
    }
}
```

## Calling convention: `app(...)->execute(...)`

Services are resolved through the container (so constructor deps autowire) and
called immediately. The same `Crupdate*` service handles both create and update —
"crupdate" = create-or-update — by accepting an optional existing model.

```php
// store
$request = app(CrupdateBackstageRequest::class)->execute($request->all());

// update — same service, pass the existing model
$request = app(CrupdateBackstageRequest::class)->execute(
    $request->all(),
    $backstageRequest,
);
```

This keeps controllers thin (see `BackstageRequestController`): authorize, then
hand off to one `execute()` call, then `return $this->success(...)`.

## Naming convention

Services are named as verbs/actions, one folder per domain:

| Service | Action |
|---------|--------|
| `App\Services\Backstage\CrupdateBackstageRequest` | create or update a request |
| `App\Services\Backstage\ApproveBackstageRequest` | approve a request |
| `App\Services\Users\UserProfileLoader` | load + normalize a profile |
| `App\Services\Users\PaginateUserProfiles` | build paginated profile query |
| `App\Services\Playlists\PaginatePlaylists` | build paginated playlist query |
| `App\Services\IncrementModelViews` | bump a view counter |

Some loaders/paginators expose intent-named methods instead of `execute()`
(e.g. `UserProfileLoader::execute()` + `toApiResource()`, or
`PaginateUserProfiles::asPaginator()`), but the rule holds: **one class, one
responsibility, invoked from the container** — not a god-service with a dozen
methods.

## Difference from the textbook service layer

| Textbook service layer | BeMusic action services |
|------------------------|-------------------------|
| `TrackService` with `play()`, `search()`, `trending()`, `delete()` | `PlayTrack`, `SearchTracks`, `GetTrendingTracks` — separate classes |
| Injected once, reused across methods | Resolved per action via `app(...)` |
| Tends to accumulate unrelated methods | Stays single-purpose (SRP by construction) |
| Often paired with a Repository abstraction | Talks to Eloquent + `Datasource` directly |

## When to add one

- New write that's more than a one-liner → a `Crupdate*` / verb-named action class.
- New list/index that needs filtering or pagination → a `Paginate*` class that
  returns a `Datasource` paginator (see datasource-pagination.md).
- Keep it in `app/Services/<Domain>/`, give it a single public method, autowire
  collaborators via the constructor, and call it with `app(Class::class)->execute(...)`.
