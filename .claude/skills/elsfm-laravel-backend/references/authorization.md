# Authorization

ELSFM authorizes every action through Policies extending `Common\Core\Policies\BasePolicy`. Controllers call `$this->authorize()`; policies decide using role-based permissions plus ownership.

## The Flow

```
Controller: $this->authorize('store', BackstageRequest::class)
   ↓ Gate resolves BackstageRequestPolicy::store($user)
   ↓ BasePolicy::hasPermission($user, 'backstageRequests.create')
   ↓ true → continue | false → 403 AuthorizationException
```

## Policy Example

```php
<?php namespace App\Policies;
use App\Models\BackstageRequest;
use App\Models\User;
use Common\Core\Policies\BasePolicy;

class BackstageRequestPolicy extends BasePolicy
{
    public function index(User $user, $userId = null) {
        return $this->hasPermission($user, 'backstageRequests.view') ||
            $user->id === (int) $userId;
    }

    public function show(User $user, BackstageRequest $backstageRequest) {
        if ($this->hasPermission($user, 'backstageRequests.update')) return true;
        return $backstageRequest->user_id === $user->id;
    }

    public function store(User $user) {
        return $this->hasPermission($user, 'backstageRequests.create');
    }

    public function destroy(User $user, $backstageRequestIds) {
        if ($user->hasPermission('backstageRequests.update')) return true;
        $dbCount = BackstageRequest::whereIn('id', $backstageRequestIds)
            ->where('user_id', $user->id)->count();
        return $dbCount === count($backstageRequestIds);
    }
}
```

## Calling from Controllers

```php
// For "no instance yet" abilities (store/index)
$this->authorize('store', BackstageRequest::class);

// For ownership-aware abilities (show/update)
$this->authorize('show', $backstageRequest);

// With extra args
$this->authorize('index', [BackstageRequest::class, $userId]);
```

## Permission Naming

Permissions are dotted strings: `resource.action`

| Ability  | Permission                  |
|----------|-----------------------------|
| view     | `backstageRequests.view`    |
| create   | `backstageRequests.create`  |
| update   | `backstageRequests.update`  |

## Checklist

- [ ] Policy extends `BasePolicy`
- [ ] Uses `hasPermission($user, 'resource.action')`
- [ ] Falls back to ownership check
- [ ] Bulk actions validate all ids
- [ ] Controller calls `$this->authorize()` before mutating
