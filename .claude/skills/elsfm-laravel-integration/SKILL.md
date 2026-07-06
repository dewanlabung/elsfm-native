---
name: elsfm-laravel-integration
description: |
  Flutter-Laravel API integration guide. Covers API client setup in Flutter, consuming
  ELSFM endpoints, authentication flow (Sanctum tokens), error handling, pagination,
  data model mapping, and best practices for mobile client integration.
---

# Flutter-Laravel Integration

How the Flutter app consumes the ELSFM Laravel backend.

## Base URL
```
https://www.elsfm.com/api/v1
```

## Authentication
```
Authorization: Bearer {token}
```

## Request/Response Examples

### Fetch Tracks
```
GET /api/v1/tracks?page=1&per_page=20

Response:
{
  "data": [
    {
      "id": 1,
      "name": "Track Name",
      "duration": 180000,
      "image": "url",
      "artists": [...],
      "album": {...}
    }
  ],
  "pagination": {
    "total": 100,
    "current_page": 1,
    "last_page": 5
  }
}
```

### Create Playlist
```
POST /api/v1/playlists
{
  "name": "My Playlist",
  "description": "..."
}

Response:
{
  "data": {
    "id": 5,
    "name": "My Playlist",
    "owner_id": 123,
    "tracks": []
  }
}
```

### Add Track to Playlist
```
POST /api/v1/playlists/{id}/tracks
{
  "track_id": 42
}
```

### Log Track Play
```
POST /api/v1/tracks/{id}/plays
```

## Flutter Integration Patterns

1. **API Client** → Dio HTTP client with interceptors
2. **Models** → Dart classes with fromJson/toJson
3. **Repositories** → Encapsulate API calls
4. **Providers** → Riverpod for state management
5. **Error Handling** → Handle 4xx/5xx responses

## Key Points

- Pagination: Use `page` + `per_page` query params
- Auth: Token stored in secure storage
- Errors: Check response.statusCode for error types
- Caching: Implement client-side caching
- Offline: Cache responses for offline access

## Reference Files

| File | Purpose |
|------|---------|
| [flutter-client-example.md](references/flutter-client-example.md) | Dio ApiClient, auth interceptor, token storage |
| [error-handling.md](references/error-handling.md) | ApiException, 422/401/403 mapping, UI handling |
| [response-mapping.md](references/response-mapping.md) | fromJson patterns, envelope unwrapping, pagination |

## When to Reference

- **Setting up the HTTP client** → flutter-client-example.md
- **Handling API errors in UI** → error-handling.md
- **Deserializing responses** → response-mapping.md
