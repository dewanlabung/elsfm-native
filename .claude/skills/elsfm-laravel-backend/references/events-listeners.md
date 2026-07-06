# Events & Listeners

ELSFM decouples side effects from core operations using Laravel's event system.
Services fire events; listeners handle persistence, notifications, and cache
invalidation independently.

## Anatomy

```
Service fires event
   ↓ EventServiceProvider::$listen maps event → listeners
   ↓ Listeners execute (sync or queued)
   ↓ Side effects: DB writes, cache bust, external calls
```

## Registering in EventServiceProvider

```php
// app/Providers/EventServiceProvider.php
protected $listen = [
    TrackWasPlayed::class => [
        IncrementTrackPlayCount::class,
        LogRecentlyPlayed::class,
    ],
    PlaylistWasUpdated::class => [
        InvalidatePlaylistCache::class,
    ],
    UserFollowed::class => [
        SendFollowNotification::class,
    ],
];
```

## Event Class

```php
<?php namespace App\Events;

use App\Models\Track;
use App\Models\User;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class TrackWasPlayed
{
    use Dispatchable, SerializesModels;

    public function __construct(
        public readonly Track $track,
        public readonly User  $user,
    ) {}
}
```

## Listener — Synchronous

```php
<?php namespace App\Listeners;

use App\Events\TrackWasPlayed;

class IncrementTrackPlayCount
{
    public function handle(TrackWasPlayed $event): void
    {
        $event->track->increment('plays');
    }
}
```

## Listener — Queued

```php
<?php namespace App\Listeners;

use App\Events\TrackWasPlayed;
use Illuminate\Contracts\Queue\ShouldQueue;

class LogRecentlyPlayed implements ShouldQueue
{
    public string $queue = 'default';

    public function handle(TrackWasPlayed $event): void
    {
        // Runs in background worker — safe for slow operations.
        $event->user->recentlyPlayed()->syncWithoutDetaching([
            $event->track->id => ['played_at' => now()],
        ]);
    }
}
```

## Firing Events from a Service

```php
class TrackService
{
    public function playTrack(Track $track, User $user): void
    {
        TrackPlay::create(['track_id' => $track->id, 'user_id' => $user->id]);
        event(new TrackWasPlayed($track, $user));  // dispatches all registered listeners
    }

    public function updatePlaylist(Playlist $playlist, array $data): Playlist
    {
        $playlist->update($data);
        event(new PlaylistWasUpdated($playlist));
        return $playlist->fresh();
    }
}
```

## Key Events in ELSFM

| Event | Listeners | Trigger |
|-------|-----------|---------|
| `TrackWasPlayed` | `IncrementTrackPlayCount`, `LogRecentlyPlayed` | `TrackService::playTrack()` |
| `PlaylistWasUpdated` | `InvalidatePlaylistCache` | `PlaylistService::update()` |
| `UserFollowed` | `SendFollowNotification` | `UserService::follow()` |
| `TrackWasImported` | `GenerateWaveform`, `NotifyAdmin` | import pipeline |

## Generating via Artisan

```bash
php artisan make:event TrackWasPlayed
php artisan make:listener IncrementTrackPlayCount --event=TrackWasPlayed
```

## Checklist

- [ ] Event uses `SerializesModels` when holding Eloquent models
- [ ] Slow/IO listeners implement `ShouldQueue`
- [ ] Event registered in `EventServiceProvider::$listen`
- [ ] Service fires event *after* the primary mutation succeeds
- [ ] Listener handles its own exceptions (queue retries apply)
