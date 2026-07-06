# Response Mapping

Patterns for deserializing the ELSFM Laravel JSON envelope into typed Dart models.

## Envelope Shape

```json
{
  "data":       { ... },      // single resource
  "data":       [ ... ],      // collection
  "pagination": {             // present on paginated list endpoints
    "total":        100,
    "per_page":     20,
    "current_page": 1,
    "last_page":    5
  },
  "message": "Success"        // human-readable; present on mutations
}
```

Named top-level keys also appear for mutations:
```json
{ "request": { ... } }   // BackstageRequest store
{ "playlist": { ... } }  // Playlist store/update
{ "track": { ... } }     // Track show
```

## fromJson Pattern

Every model exposes a `factory Model.fromJson(Map<String, dynamic> json)` constructor.
Fields are mapped defensively — never assume a field is non-null from the server.

```dart
// lib/data/models/track.dart

class Track {
  const Track({
    required this.id,
    required this.name,
    required this.duration,
    this.image,
    this.artists = const [],
    this.album,
  });

  final int id;
  final String name;
  final int duration;       // milliseconds
  final String? image;
  final List<Artist> artists;
  final Album? album;

  factory Track.fromJson(Map<String, dynamic> json) => Track(
    id:       json['id'] as int,
    name:     json['name'] as String,
    duration: (json['duration'] as num).toInt(),
    image:    json['image'] as String?,
    artists:  (json['artists'] as List? ?? [])
                  .map((e) => Artist.fromJson(e as Map<String, dynamic>))
                  .toList(),
    album:    json['album'] != null
                  ? Album.fromJson(json['album'] as Map<String, dynamic>)
                  : null,
  );

  Map<String, dynamic> toJson() => {
    'id':       id,
    'name':     name,
    'duration': duration,
    if (image != null) 'image': image,
  };
}
```

## Unwrapping the Envelope

```dart
// Single resource — unwrap named key
factory Track.fromEnvelope(Map<String, dynamic> body) =>
    Track.fromJson(body['track'] as Map<String, dynamic>);

// Collection — unwrap 'data' array
static List<Track> listFromEnvelope(Map<String, dynamic> body) =>
    (body['data'] as List)
        .map((e) => Track.fromJson(e as Map<String, dynamic>))
        .toList();
```

## Pagination Model

```dart
// lib/data/models/paginated_result.dart

class PaginatedResult<T> {
  const PaginatedResult({
    required this.data,
    required this.total,
    required this.currentPage,
    required this.lastPage,
    required this.perPage,
  });

  final List<T> data;
  final int total;
  final int currentPage;
  final int lastPage;
  final int perPage;

  bool get hasNextPage => currentPage < lastPage;

  factory PaginatedResult.fromJson(
    Map<String, dynamic> body,
    T Function(Map<String, dynamic>) fromItem,
  ) {
    final p = body['pagination'] as Map<String, dynamic>;
    return PaginatedResult(
      data:        (body['data'] as List)
                       .map((e) => fromItem(e as Map<String, dynamic>))
                       .toList(),
      total:       p['total'] as int,
      currentPage: p['current_page'] as int,
      lastPage:    p['last_page'] as int,
      perPage:     p['per_page'] as int,
    );
  }
}
```

## Using PaginatedResult in a Repository

```dart
Future<PaginatedResult<Track>> fetchTracks({int page = 1, int perPage = 20}) async {
  final body = await _client.get('/tracks', query: {
    'page': page,
    'per_page': perPage,
  });
  return PaginatedResult.fromJson(body, Track.fromJson);
}

Future<PaginatedResult<Playlist>> fetchUserPlaylists({int page = 1}) async {
  final body = await _client.get('/playlists', query: {'page': page});
  return PaginatedResult.fromJson(body, Playlist.fromJson);
}
```

## Mutation Response Patterns

```dart
// Playlist create: body contains {"playlist": {...}}
Future<Playlist> createPlaylist(String name, {String? description}) async {
  final body = await _client.post('/playlists', body: {
    'name': name,
    if (description != null) 'description': description,
  });
  return Playlist.fromJson(body['playlist'] as Map<String, dynamic>);
}

// Track add to playlist — no body returned; 200 OK is success
Future<void> addTrack(int playlistId, int trackId) async {
  await _client.post('/playlists/$playlistId/tracks', body: {'track_id': trackId});
}
```

## Key Points

- Collect envelope key names from `flutter-client-example.md` (`pagination`, `track`, `playlist`, `request`).
- Always cast list items with `e as Map<String, dynamic>` — never rely on implicit casts.
- Use `(json['field'] as num).toInt()` for numeric fields that may arrive as `int` or `double`.
- Keep `fromJson` pure (no side effects) so it is safe to call in tests without a running app.
