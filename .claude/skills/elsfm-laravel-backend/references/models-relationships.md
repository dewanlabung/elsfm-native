# Models & Relationships

Eloquent models in BeMusic (ELSFM). The key architectural fact: **most models extend
base classes from the `common/foundation` package**, not Laravel's `Model` directly.
The thin `app/Models/*` class only adds project-specific relations and overrides.

## Base Class Inheritance

| App model | Extends | Where base lives |
|-----------|---------|------------------|
| `App\Models\User` | `Common\Auth\BaseUser` | `common/foundation/src/Auth/BaseUser.php` |
| `App\Models\Channel` | `Common\Channels\BaseChannel` | `common/foundation/src/Channels/BaseChannel.php` |
| `App\Models\BackstageRequest` | `Common\Core\BaseModel` | `common/foundation/src/Core/BaseModel.php` |

`BaseUser`/`BaseChannel` themselves extend `Common\Core\BaseModel`. The base classes
carry the bulk of the behavior (relations, search, permissions, casts); the `app/`
subclass is intentionally small.

## User

`User` extends `BaseUser`, which already defines auth, permissions, billing,
file entries, sessions, and the follow relations. The `app/Models/User.php`
subclass only adds music-specific relations.

```php
// app/Models/User.php
class User extends BaseUser
{
    use Notifiable, HasApiTokens, HasFactory;

    const MODEL_TYPE = 'user';

    public function profile(): HasOne
    {
        return $this->hasOne(ProfileDetails::class);
    }

    public function playlists(): BelongsToMany
    {
        return $this->belongsToMany(Playlist::class);
    }

    // polymorphic: profile links attach via "linkeable"
    public function links(): MorphMany
    {
        return $this->morphMany(ProfileLink::class, 'linkeable');
    }
}
```

### Follows are a plain pivot, NOT polymorphic

Defined on `BaseUser`, backed by the `follows` table
(`follower_id`, `followed_id`, unique together). There is no `followers` schema
with `user_id`/`followed_id` — use the real columns below.

```php
// Common\Auth\BaseUser
public function followedUsers(): BelongsToMany
{
    return $this->belongsToMany(User::class, 'follows', 'follower_id', 'followed_id')
        ->compact();
}

public function followers(): BelongsToMany
{
    return $this->belongsToMany(User::class, 'follows', 'followed_id', 'follower_id')
        ->compact();
}
```

`->compact()` is a local scope on `BaseUser` that selects only
`id, image, email, name, username` — used to keep follower lists lightweight.

Inherited from `BaseUser` (do not redeclare these in `app/`): `roles()`,
`permissions` (via `HasPermissionsRelation`), `notificationSubscriptions()`,
`entries()` (polymorphic file entries via `file_entry_models` / `model`),
`social_profiles()`, `userSessions()`, `subscriptions` (via `Billable`).

## Channel (polymorphic content container)

`Channel` is how BeMusic groups heterogeneous content (tracks, albums, playlists,
users) into a single ordered list. It uses **`morphedByMany` with the `channelable`
morph name** and a `channelables` pivot carrying `order`.

```php
// app/Models/Channel.php
class Channel extends BaseChannel
{
    public function users(): MorphToMany
    {
        return $this->morphedByMany(User::class, 'channelable')
            ->withPivot(['id', 'channelable_id', 'order']);
    }

    public function playlists(): MorphToMany
    {
        return $this->morphedByMany(Playlist::class, 'channelable')
            ->withPivot(['id', 'channelable_id', 'order']);
    }
}
```

`BaseChannel` also defines `user()` (belongsTo owner), `channels()` (nested
channels via the same morph), and `updateContentFromExternal()` which groups
content by `model_type`, pluralizes it (`track` -> `tracks`) and calls the
matching morph relation with `syncWithoutDetaching`.

## BackstageRequest

```php
// app/Models/BackstageRequest.php
class BackstageRequest extends BaseModel
{
    const MODEL_TYPE = 'backstageRequest';

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function artist(): BelongsTo
    {
        return $this->belongsTo(Artist::class);
    }

    // stored as JSON text column
    public function getDataAttribute()
    {
        return $this->attributes['data']
            ? json_decode($this->attributes['data'], true)
            : $this->attributes['data'];
    }
}
```

## Track / Album / Artist / Playlist

These music models are referenced from this codebase (e.g.
`BackstageRequest::artist()`, `Channel::playlists()`, `User::playlists()`) but
their class definitions ship in the BeMusic premium package, not in this script's
`app/Models/`. They follow the same conventions: extend `Common\Core\BaseModel`,
declare a `MODEL_TYPE` constant, implement `toNormalizedArray()`,
`toSearchableArray()`, and a static `filterableFields()` (used by `Datasource`).

Typical relationships (as consumed here):

| Model | Relationships |
|-------|---------------|
| Track | `album()` belongsTo, `artists()` belongsToMany, `genres()`/`genre` |
| Album | `artists()` belongsToMany, `tracks()` hasMany |
| Artist | `albums()`, `tracks()` |
| Playlist | `owner` (User), `tracks()` belongsToMany with pivot order |

When adding a relation that touches one of these, declare it on the local `app/`
subclass if one exists, otherwise extend the package model following the
`common-foundation` pattern (see the integration skill).

## The `model_type` / normalization convention

Every model exposes a `MODEL_TYPE` constant and a `toNormalizedArray()` returning
`{ id, name, description, image, model_type }`. This is what powers polymorphic
channel content and search results — a single list can mix users, playlists,
albums, and tracks and the frontend dispatches on `model_type`.

```php
public function toNormalizedArray(): array
{
    return [
        'id' => $this->id,
        'name' => $this->name,
        'image' => $this->image,
        'model_type' => static::MODEL_TYPE,
    ];
}
```

## Querying (use Datasource, see datasource-pagination.md)

```php
// Eager loading + filtering/pagination goes through Datasource, not Resources
$builder = $this->backstageRequest->with(['user', 'artist']);
$pagination = (new Datasource($builder, $request->all()))->paginate();
```
