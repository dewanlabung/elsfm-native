# Flutter ApiClient Example

Complete Dart HTTP client for ELSFM Laravel API with Sanctum token auth.

## Base Configuration

```dart
class ApiConfig {
  static const String baseUrl = 'https://www.elsfm.com/api/v1';
  static const Duration timeout = Duration(seconds: 20);
}
```

## The ApiClient (Dio)

```dart
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiClient {
  ApiClient({Dio? dio, FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage(),
        _dio = dio ?? Dio(BaseOptions(
          baseUrl: ApiConfig.baseUrl,
          connectTimeout: ApiConfig.timeout,
          receiveTimeout: ApiConfig.timeout,
          headers: {'Accept': 'application/json'},
        )) {
    _dio.interceptors.add(_authInterceptor());
  }

  final Dio _dio;
  final FlutterSecureStorage _storage;

  Interceptor _authInterceptor() => InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _storage.read(key: 'auth_token');
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
      );

  Future<Map<String, dynamic>> get(String path, {Map<String, dynamic>? query}) async {
    return _send(() => _dio.get(path, queryParameters: query));
  }

  Future<Map<String, dynamic>> post(String path, {Map<String, dynamic>? body}) async {
    return _send(() => _dio.post(path, data: body));
  }

  Future<Map<String, dynamic>> put(String path, {Map<String, dynamic>? body}) {
    return _send(() => _dio.put(path, data: body));
  }

  Future<Map<String, dynamic>> delete(String path) {
    return _send(() => _dio.delete(path));
  }

  Future<Map<String, dynamic>> _send(Future<Response> Function() request) async {
    try {
      final response = await request();
      return (response.data as Map).cast<String, dynamic>();
    } on DioException catch (e) {
      throw ApiException.fromDio(e);
    }
  }
}
```

## Token Persistence

```dart
await _storage.write(key: 'auth_token', value: token);
await _storage.delete(key: 'auth_token');
```

## Using in Repository

```dart
class TrackRepository {
  TrackRepository(this._client);
  final ApiClient _client;

  Future<TrackPage> fetchTracks({int page = 1, int perPage = 20}) async {
    final json = await _client.get('/tracks', query: {
      'page': page,
      'per_page': perPage,
    });
    return TrackPage.fromJson(json['pagination'] as Map<String, dynamic>);
  }

  Future<void> logPlay(int trackId) async {
    await _client.post('/tracks/$trackId/plays');
  }

  Future<Playlist> createPlaylist(String name, {String? description}) async {
    final json = await _client.post('/playlists', body: {
      'name': name,
      if (description != null) 'description': description,
    });
    return Playlist.fromJson(json['playlist'] as Map<String, dynamic>);
  }
}
```

## Key Points

- Named envelope keys (`pagination`, `track`, `request`, `playlist`)
- Send `Accept: application/json` for JSON validation errors
- Keep one `ApiClient` instance; don't recreate per call
