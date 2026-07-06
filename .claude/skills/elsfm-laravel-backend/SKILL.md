---
name: elsfm-laravel-backend
description: |
  Laravel backend architecture for ELSFM music streaming API. Covers Eloquent models
  (User, Track, Album, Artist, Playlist, Channel), service layer patterns, controller
  structure, API middleware (authentication, validation, authorization), database
  relationships, and event-driven architecture. Use when building backend features,
  understanding data models, implementing business logic, or extending the API.
---

# ELSFM Laravel Backend

Production music streaming API built with Laravel 12 (or 11), using Eloquent ORM, services, and RESTful principles.

## Quick Start

```php
// Models (Eloquent)
User::with('playlists', 'followers')->find($id);
Track::where('genre_id', $genreId)->paginate(20);
Playlist::where('owner_id', auth()->id())->get();

// Services (Business Logic)
$userProfile = app(UserProfileLoader::class)->loadProfile($userId);
$tracks = app(TrackRepository::class)->getTracks(page: 1, perPage: 20);

// Controllers (HTTP)
class TrackController extends Controller {
  public function index() {
    return TrackResource::collection(Track::paginate());
  }
}

// Routes
Route::get('/api/v1/tracks', [TrackController::class, 'index']);
```

## Architecture

### Layers

```
HTTP (Routes, Controllers, Requests)
  ↓
Services (Business Logic)
  ↓
Models (Eloquent ORM)
  ↓
Database
```

### Directory Structure

```
app/
├── Http/
│   ├── Controllers/          # HTTP handlers
│   │   ├── TrackController
│   │   ├── AlbumController
│   │   ├── PlaylistController
│   │   ├── UserProfile/
│   │   └── Search/
│   ├── Requests/             # Form validation
│   │   ├── ModifyTracks
│   │   └── ModifyPlaylists
│   └── Middleware/           # Auth, CORS, etc.
├── Models/                   # Eloquent models
│   ├── Track
│   ├── Album
│   ├── Artist
│   ├── Playlist
│   ├── User
│   └── Channel
├── Services/                 # Business logic
│   ├── TrackService
│   ├── PlaylistService
│   └── UserLibraryService
├── Repositories/             # Data access (optional)
│   └── TrackRepository
├── Policies/                 # Authorization
│   ├── TrackPolicy
│   └── PlaylistPolicy
├── Events/                   # Domain events
│   ├── TrackWasPlayed
│   └── PlaylistWasUpdated
└── Listeners/                # Event handlers
    └── IncrementTrackPlays
```

## Core Concepts

### Models (See [models-relationships.md](references/models-relationships.md))

```php
// Users have many playlists
class User extends Model {
  public function playlists() {
    return $this->hasMany(Playlist::class);
  }
}

// Tracks belong to albums and artists
class Track extends Model {
  public function album() {
    return $this->belongsTo(Album::class);
  }
  
  public function artists() {
    return $this->belongsToMany(Artist::class);
  }
}
```

### Services (See [services-pattern.md](references/services-pattern.md))

```php
// Encapsulate business logic
class TrackService {
  public function playTrack(Track $track, User $user): void {
    TrackPlay::create([
      'track_id' => $track->id,
      'user_id' => $user->id,
    ]);
    
    event(new TrackWasPlayed($track, $user));
  }
}
```

### Controllers (See [controllers-pattern.md](references/controllers-pattern.md))

```php
// Thin controllers - delegate to services
class TrackController extends Controller {
  public function __construct(private TrackService $trackService) {}

  public function play(Track $track) {
    $this->trackService->playTrack($track, auth()->user());
    return response()->json(['message' => 'Playing']);
  }
}
```

### API Endpoints (See [elsfm-api-endpoints](../../elsfm-api-endpoints/SKILL.md))

All endpoints follow `/api/v1/*` prefix with RESTful conventions:

```
GET    /api/v1/tracks              # List tracks (paginated)
GET    /api/v1/tracks/{id}         # Get track details
POST   /api/v1/tracks              # Create (admin only)
PUT    /api/v1/tracks/{id}         # Update (admin only)
DELETE /api/v1/tracks/{id}         # Delete (admin only)

GET    /api/v1/playlists           # User playlists
POST   /api/v1/playlists           # Create playlist
PUT    /api/v1/playlists/{id}      # Update playlist
DELETE /api/v1/playlists/{id}      # Delete playlist
```

## Key Features

- **Authentication:** Sanctum API tokens + session auth
- **Authorization:** Policies + Gate (who can do what)
- **Validation:** Form Requests (validate input)
- **API Resources:** Transform models to JSON
- **Events:** Decouple features with Laravel events
- **Queues:** Background jobs (imports, notifications)

## Development Workflow

1. **Define Model** → Create migration, Eloquent model
2. **Add Service** → Business logic here
3. **Create Controller** → Use service, return Resource
4. **Define Route** → Map to controller
5. **Write Tests** → Unit + feature tests
6. **Add Policy** → Authorization rules

## Reference Files

| File | Purpose |
|------|---------|
| [models-relationships.md](references/models-relationships.md) | Eloquent models, relationships, queries |
| [services-pattern.md](references/services-pattern.md) | Service layer, dependency injection |
| [controllers-pattern.md](references/controllers-pattern.md) | Controller structure, requests, responses |
| [elsfm-api-endpoints skill](../../elsfm-api-endpoints/SKILL.md) | Complete API endpoint reference |
| [authorization.md](references/authorization.md) | Policies, gates, permission checks |
| [events-listeners.md](references/events-listeners.md) | Event-driven architecture |

## When to Reference

- **Building new endpoint** → controllers-pattern.md
- **Adding model/relationship** → models-relationships.md
- **Implementing business logic** → services-pattern.md
- **Understanding permission** → authorization.md
- **API integration** → elsfm-api-endpoints/SKILL.md
- **Decoupling features** → events-listeners.md

## Testing

```bash
# Run tests
php artisan test

# With coverage
php artisan test --coverage

# Specific test
php artisan test tests/Feature/TrackControllerTest.php
```

## Commands

```bash
# Generate code
php artisan make:model Track -m  # Model + migration
php artisan make:controller TrackController --api
php artisan make:request ModifyTracks
php artisan make:policy TrackPolicy --model=Track

# Database
php artisan migrate
php artisan db:seed
php artisan tinker
```
