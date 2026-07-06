# Error Handling

Maps Laravel HTTP status codes to typed Dart exceptions so Flutter can
present the correct UI without inspecting raw status codes at every call site.

## ApiException

```dart
// lib/core/api/api_exception.dart

import 'package:dio/dio.dart';

enum ApiErrorType { unauthorized, forbidden, validation, notFound, server, network, unknown }

class ApiException implements Exception {
  const ApiException({
    required this.type,
    required this.message,
    this.validationErrors = const {},
    this.statusCode,
  });

  final ApiErrorType type;
  final String message;
  final Map<String, List<String>> validationErrors; // 422 field errors
  final int? statusCode;

  /// Build from a Dio error — the single conversion point.
  factory ApiException.fromDio(DioException e) {
    final response = e.response;
    if (response == null) {
      return ApiException(
        type: ApiErrorType.network,
        message: 'No internet connection. Check your network and try again.',
      );
    }

    final body = response.data is Map ? response.data as Map<String, dynamic> : {};
    final serverMessage = body['message'] as String? ?? 'An error occurred.';

    switch (response.statusCode) {
      case 401:
        return ApiException(
          type: ApiErrorType.unauthorized,
          message: 'Session expired. Please log in again.',
          statusCode: 401,
        );
      case 403:
        return ApiException(
          type: ApiErrorType.forbidden,
          message: serverMessage,
          statusCode: 403,
        );
      case 404:
        return ApiException(
          type: ApiErrorType.notFound,
          message: serverMessage,
          statusCode: 404,
        );
      case 422:
        // Laravel validation: {"message":"...", "errors":{"field":["msg"]}}
        final raw = body['errors'] as Map<String, dynamic>? ?? {};
        final errors = raw.map(
          (k, v) => MapEntry(k, (v as List).cast<String>()),
        );
        return ApiException(
          type: ApiErrorType.validation,
          message: serverMessage,
          validationErrors: errors,
          statusCode: 422,
        );
      default:
        return ApiException(
          type: ApiErrorType.server,
          message: 'Server error (${response.statusCode}). Try again later.',
          statusCode: response.statusCode,
        );
    }
  }

  @override
  String toString() => 'ApiException(${type.name}, $statusCode): $message';
}
```

## Handling in Repositories

```dart
class TrackRepository {
  TrackRepository(this._client);
  final ApiClient _client;

  Future<List<Track>> fetchTracks({int page = 1}) async {
    try {
      final json = await _client.get('/tracks', query: {'page': page});
      return (json['data'] as List)
          .map((e) => Track.fromJson(e as Map<String, dynamic>))
          .toList();
    } on ApiException {
      rethrow; // Let the provider / UI layer decide what to show.
    }
  }
}
```

## Handling in Riverpod Providers

```dart
final tracksProvider = FutureProvider<List<Track>>((ref) async {
  final repo = ref.watch(trackRepositoryProvider);
  return repo.fetchTracks();
  // Errors bubble as AsyncError — .when(error:) catches them.
});
```

## Handling in UI (AsyncValue.when)

```dart
ref.watch(tracksProvider).when(
  loading: () => const CircularProgressIndicator(),
  error: (e, _) {
    if (e is ApiException && e.type == ApiErrorType.unauthorized) {
      // Redirect to login instead of showing an error widget.
      WidgetsBinding.instance.addPostFrameCallback((_) => context.go('/login'));
      return const SizedBox.shrink();
    }
    return _ErrorWidget(
      message: e is ApiException ? e.message : 'Something went wrong.',
      onRetry: () => ref.invalidate(tracksProvider),
    );
  },
  data: (tracks) => TrackListView(tracks: tracks),
);
```

## 401 Auto-Refresh via Interceptor

```dart
// In ApiClient._authInterceptor()
onError: (error, handler) async {
  if (error.response?.statusCode == 401) {
    await ref.read(authNotifierProvider.notifier).signOut();
    // Navigate to login — handled by router redirect guard.
  }
  handler.next(error);
},
```

## Status → Type Mapping

| HTTP | `ApiErrorType` | Typical UI Action |
|------|---------------|-------------------|
| 401  | `unauthorized` | Redirect to `/login` |
| 403  | `forbidden` | Show "Not allowed" message |
| 404  | `notFound` | Show "Not found" inline |
| 422  | `validation` | Display per-field errors on form |
| 5xx  | `server` | Show retry widget |
| Network | `network` | Show offline banner |
