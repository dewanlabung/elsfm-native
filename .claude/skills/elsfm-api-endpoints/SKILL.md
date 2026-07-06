---
name: elsfm-api-endpoints
description: |
  Complete API endpoint reference for ELSFM backend. Covers tracks, albums, artists,
  playlists, users, channels, search, and authentication endpoints. Includes request/response
  formats, pagination, filtering, authentication requirements. Use as lookup when consuming
  API from Flutter or documenting client integration.
---

# API Endpoints Reference

## Prefix
All endpoints: `/api/v1/*`

## Tracks
```
GET    /tracks              # List (paginated)
GET    /tracks/{id}         # Details
POST   /tracks              # Create (admin)
PUT    /tracks/{id}         # Update
DELETE /tracks/{id}         # Delete
POST   /tracks/{id}/plays   # Log play
```

## Albums
```
GET    /albums              # List
GET    /albums/{id}         # Details + tracks
GET    /artists/{id}/albums # Artist albums
```

## Artists
```
GET    /artists             # List
GET    /artists/{id}        # Details
GET    /artists/{id}/tracks # Artist tracks
GET    /artists/{id}/albums # Artist albums
```

## Playlists
```
GET    /playlists           # User playlists
POST   /playlists           # Create
GET    /playlists/{id}      # Details
PUT    /playlists/{id}      # Update
DELETE /playlists/{id}      # Delete
POST   /playlists/{id}/tracks      # Add track
DELETE /playlists/{id}/tracks/{trackId}  # Remove track
```

## Users
```
GET    /users/{id}/profile         # Profile
GET    /users/{id}/playlists       # User playlists
GET    /users/{id}/followers       # Followers
GET    /users/{id}/followed-users  # Following
POST   /users/{id}/follow          # Follow
POST   /users/{id}/unfollow        # Unfollow
```

## Search
```
GET /search?q=query    # Multi-entity search
```

## Channels
```
GET    /channel              # List
GET    /channel/{id}         # Details
POST   /channel              # Create
PUT    /channel/{id}         # Update
DELETE /channel/{ids}        # Delete
```

## Response Format
```json
{
  "data": {...},
  "pagination": {
    "total": 100,
    "per_page": 20,
    "current_page": 1,
    "last_page": 5
  },
  "message": "Success"
}
```

## Pagination
```
?page=1&per_page=20
```

## Filters
```
?genre_id=5&created_at=2024-01-01
```
