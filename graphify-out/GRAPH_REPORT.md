# Graph Report - elsfm-native  (2026-07-23)

## Corpus Check
- 356 files · ~143,106 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2609 nodes · 4459 edges · 211 communities (155 shown, 56 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 478 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `eb92646e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- PlayerViewModel
- SubscriptionViewModel
- ArtistDetailViewModel
- PlaylistViewModelTest
- SearchUiState
- NotificationsViewModel
- LibraryViewModel
- FakePlayerController
- AppDatabase
- CommentsViewModel
- UserApi
- StorageViewModel
- AuthApi
- UserProfileViewModel
- ELSFM Laravel Backend Skill
- PlaylistApi
- DownloadedTrack
- ApiResult
- PlaylistViewModel
- AlbumViewModel
- ListeningHistoryViewModel
- DownloadRepository
- ThemeViewModel
- DiscoveryViewModel
- ArtistDetailScreen.kt
- SearchResult
- AuthRepository
- Media3PlayerController
- FakeAuthApi
- FakeUserDao
- ArtistApi.kt
- SessionManager
- LibraryScreen.kt
- LibraryState
- DownloadsViewModel
- ArtistCard
- PlayerMenuEvent
- UserEntity
- RepostApi
- PlayerController
- FakeAccountApi
- .viewModel
- FakePlayerController
- SettingsScreen
- AccountApi
- TokenStore
- LoginFakeAuthApi
- FakePlayerController
- Intent
- .clientReturning
- LoginEvent
- LibraryApiRepositoryImpl
- AuthApiLike
- User
- LikedSongsViewModel
- SettingsViewModel
- PlaybackService
- ProfileScreen
- ProfileApi
- SignupEvent
- ChannelDetailViewModel
- DiscoveryContent
- ChangePasswordViewModel
- withRetry
- elsfmJson
- .buildRepository
- AuthApi / AuthApiLike (login endpoint client)
- ElsfmNavHost
- AlbumCard
- MediaItemFactoryTest
- UserApi.kt
- ChannelDetailScreen.kt
- .viewModel
- .viewModel
- ProfileViewModel
- StartDestinationViewModel
- LibraryCacheDao
- PlayHistoryApi
- .clientReturning
- ChannelApi
- .clientReturning
- LoginResponseSerializationTest
- EmailVerificationEvent
- DownloadsEvent
- SleepTimer
- AccountViewModel
- FakeProfileCacheDao
- DownloadQuality
- ApiErrorHandlerTest
- GoogleSignInServiceLike
- ViewModel
- FakeUserDao
- ElsfmApplication
- Phase 1 Foundation Implementation Plan
- UserDao
- Channel
- DiscoveryScreenTest
- ConnectivityViewModel
- .downSwipeTriggersOnExpandClicked
- PlayerScreen
- ELSFM Native App Feature Inventory
- ElsfmTheme
- ShakeDetector
- ShakeSensitivity
- ProfileApi.kt
- PlaylistCard
- .uploadAvatar
- ImageUrlSerializer
- NullSafeLongSerializer
- ChannelApi.kt
- LoginUiState
- RecentlyPlayedSection
- PlayerState
- .newApplication
- HomeViewModel
- TrackListItem
- .toChannelContentResult
- Track (Eloquent model)
- Spacer
- PopularSongRow
- DownloadsScreen
- DeepLinkTrackViewModel
- AutoCacheWorker
- HeadsetEventMonitor
- RecentTracksStore
- LaravelValidationError.kt
- FakeUserDao
- RecentlyPlayedTrackItem
- Flutter-Laravel Integration Skill
- LoginFlowInstrumentedTest
- LocalRecommendationEngine
- TrackSerializationTest
- ChannelApi + SearchApi + ArtistApi (Phase 3 network layer)
- DownloadTab
- ELSFM Capacitor Build Skill
- ELSFM Auth Hero Image (Hands Playing Harp, Nepali Text: Songs of Zion)
- .downloadTrack
- ChannelSerializationTest
- SearchResultSerializationTest
- UserProfileSerializationTest
- dummyHttpClient
- LoggingBehaviorTest
- Material 3 Full Design System Integration (Phase 5 redesign)
- PasswordSaver
- DownloadItem
- .bindPlayerController
- UserSessionInfo
- UserProfile
- gradlew
- Album.kt
- LoginResponse.kt
- Permission.kt
- Playlist.kt
- Tag.kt
- ElsfmApiConfig.kt
- DiscoveryState.kt
- ELSFM Datasource Pagination Reference
- PlaylistWasUpdated (Laravel Event)
- Channel (Eloquent model, extends BaseChannel)
- ELSFM Parallel Platform Agents Skill
- GitHub Release + APK Sideload Process
- Google Play Store Submission Checklist (AAB, icon, screenshots, data safety)
- TrackListApi (GET /api/v1/playlist/{id} track listing)
- DownloadManager + DownloadViewModel (offline track storage)
- ProfileViewModel (user profile + recently played + edit)
- Playlist
- TrackListApi
- elsfmJson
- ListeningHistoryViewModelTest
- .paginatedViewModel
- DefaultNetworkMonitor
- DispatcherProvider
- fakeDownloadRepository
- ArtistCard
- SubscriptionScreen.kt
- RepostApi
- .controllerReturning
- UserApiLike
- SortBy
- ArtistFollower.kt
- AuthPluginConfig
- Album
- com
- Album
- Playlist
- Playlist
- Playlist

## God Nodes (most connected - your core abstractions)
1. `ApiResult` - 99 edges
2. `UserApi` - 68 edges
3. `elsfmJson()` - 53 edges
4. `PlayerViewModel` - 36 edges
5. `UserApiTest` - 34 edges
6. `UserEntity` - 31 edges
7. `FakePlayerController` - 31 edges
8. `PlaylistApi` - 29 edges
9. `SessionManager` - 28 edges
10. `Media3PlayerController` - 27 edges

## Surprising Connections (you probably didn't know these)
- `ElsfmNavHost()` --calls--> `ArtistDetailScreen()`  [INFERRED]
  app/src/main/kotlin/com/elsfm/mobile/ElsfmNavHost.kt → feature/artist/src/main/kotlin/com/elsfm/mobile/feature/artist/ArtistDetailScreen.kt
- `ElsfmNavHost()` --calls--> `DiscoveryScreen()`  [INFERRED]
  app/src/main/kotlin/com/elsfm/mobile/ElsfmNavHost.kt → feature/discovery/src/main/kotlin/com/elsfm/mobile/feature/discovery/DiscoveryScreen.kt
- `TracksTab()` --calls--> `TrackContextMenu()`  [INFERRED]
  feature/artist/src/main/kotlin/com/elsfm/mobile/feature/artist/ArtistDetailScreen.kt → core/designsystem/src/main/kotlin/com/elsfm/mobile/core/designsystem/TrackContextMenu.kt
- `AlbumTrackRow()` --calls--> `TrackContextMenu()`  [INFERRED]
  feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/AlbumScreen.kt → core/designsystem/src/main/kotlin/com/elsfm/mobile/core/designsystem/TrackContextMenu.kt
- `LikedSongTrackRow()` --calls--> `TrackContextMenu()`  [INFERRED]
  feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/LikedSongsScreen.kt → core/designsystem/src/main/kotlin/com/elsfm/mobile/core/designsystem/TrackContextMenu.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **ELSFM Laravel Layered Architecture (Controllers → Services → Models → DB)** — _claude_skills_elsfm_laravel_backend_references_controllers_pattern_backstagerequestcontroller, _claude_skills_elsfm_laravel_backend_references_services_pattern_crupdatebackstagerequest, _claude_skills_elsfm_laravel_backend_references_models_relationships_backstagerequest, _claude_skills_elsfm_laravel_backend_references_datasource_pagination_datasource [EXTRACTED 1.00]
- **Android Smart Playback Feature Group (shake-skip, headset monitor, auto-recommend)** — _claude_skills_elsfm_android_build_skill_playbackservice, _claude_skills_elsfm_android_build_skill_shakedetector, _claude_skills_elsfm_android_build_skill_headseteventmonitor, _claude_skills_elsfm_android_build_skill_localrecommendationengine, _claude_skills_elsfm_android_build_skill_recenttracksstore [EXTRACTED 1.00]
- **Flutter API Integration Stack (ApiClient → Repository → PaginatedResult → ApiException)** — _claude_skills_elsfm_laravel_integration_references_flutter_client_example_apiclient, _claude_skills_elsfm_laravel_integration_references_flutter_client_example_trackrepository, _claude_skills_elsfm_laravel_integration_references_response_mapping_paginatedresult, _claude_skills_elsfm_laravel_integration_references_error_handling_apiexception [EXTRACTED 1.00]
- **ELSFM Native 5-Phase Development Roadmap** — docs_superpowers_plans_2026_07_04_phase1_foundation_plan_phase1plan, docs_superpowers_plans_2026_07_05_phase2_player_plan_phase2plan, docs_superpowers_plans_2026_07_05_phase3_library_plan_phase3plan, docs_superpowers_plans_2026_07_05_phase4_advanced_features_plan_phase4plan, docs_superpowers_plans_2026_07_06_phase5_navigation_ui_redesign_phase5plan [EXTRACTED 1.00]
- **Sanctum Bearer Token Authentication Component Flow** — docs_superpowers_plans_2026_07_04_phase1_foundation_plan_tokenstore, docs_superpowers_plans_2026_07_04_phase1_foundation_plan_sessionmanager, docs_superpowers_plans_2026_07_04_phase1_foundation_plan_authplugin, docs_superpowers_plans_2026_07_04_phase1_foundation_plan_authapi, docs_superpowers_plans_2026_07_04_phase1_foundation_plan_authrepository [INFERRED 0.95]
- **ELSFM App Brand Identity - Songs of Zion Harp Imagery** — app_src_main_res_drawable_auth_hero_image, app_src_main_res_drawable_ic_launcher_image, feature_auth_src_main_res_drawable_auth_hero_image [EXTRACTED 1.00]

## Communities (211 total, 56 thin omitted)

### Community 0 - "PlayerViewModel"
Cohesion: 0.23
Nodes (5): fakePlayerMenuRepository(), fakeUserApi(), FakeUserDao, HttpStatusCode, PlayerViewModelTest

### Community 1 - "SubscriptionViewModel"
Cohesion: 0.07
Nodes (22): BillingApi, BillingPriceDto, BillingProductDto, BillingSubscription, BillingUserResponse, CancelSubscriptionRequest, SubscriptionDto, toBillingSubscription() (+14 more)

### Community 2 - "ArtistDetailViewModel"
Cohesion: 0.06
Nodes (24): FollowStateDao, Flow, FollowedArtistEntity, FollowStateRepository, Flow, ArtistDetailState, ArtistTab, ABOUT (+16 more)

### Community 4 - "SearchUiState"
Cohesion: 0.20
Nodes (5): StateFlow, ViewModel, SearchViewModel, HttpStatusCode, SearchViewModelTest

### Community 5 - "NotificationsViewModel"
Cohesion: 0.05
Nodes (26): AppNotification, NotificationData, NotificationLine, MarkAsReadRequest, NotificationsApi, NotificationsPagination, NotificationsResponse, HttpClient (+18 more)

### Community 6 - "LibraryViewModel"
Cohesion: 0.13
Nodes (10): LibrarySections, StateFlow, ViewModel, LibraryFilter, ALBUMS, ALL, ARTISTS, CHANNELS (+2 more)

### Community 7 - "FakePlayerController"
Cohesion: 0.05
Nodes (23): LyricLine, TrackLyrics, LyricsApi, currentLineIndex(), LyricsErrorState(), LyricsScreen(), PlainLyricsList(), SyncedLyricsList() (+15 more)

### Community 8 - "AppDatabase"
Cohesion: 0.06
Nodes (12): PersistedPlaybackState, PlaybackStateStore, AppDatabase, PlaybackStateDao, TokenDao, DatabaseModule, Context, PlaybackStateEntity (+4 more)

### Community 9 - "CommentsViewModel"
Cohesion: 0.07
Nodes (19): Comment, CommentUser, CommentApi, CommentsPagination, CommentsResponse, CreateCommentRequest, CreateCommentResponse, UpdateCommentRequest (+11 more)

### Community 10 - "UserApi"
Cohesion: 0.18
Nodes (3): UserApi, HttpStatusCode, UserApiTest

### Community 11 - "StorageViewModel"
Cohesion: 0.08
Nodes (11): CacheEntry, StorageState, StateFlow, ViewModel, StorageViewModel, FakeDiscoveryCacheDao, FakeDownloadedTrackDao, FakeLibraryCacheDao (+3 more)

### Community 12 - "AuthApi"
Cohesion: 0.12
Nodes (14): CompletePasswordResetRequest, PasswordResetRequest, RegisterBootstrapData, RegisterRequest, RegisterResponse, AuthApi, EmailVerifyRequest, errorsFrom() (+6 more)

### Community 13 - "UserProfileViewModel"
Cohesion: 0.07
Nodes (21): FollowUser, UserListTab(), UserProfileErrorState(), UserProfileHeader(), UserProfileScreen(), UserProfileTabRow(), UserRow(), UserProfileState (+13 more)

### Community 14 - "ELSFM Laravel Backend Skill"
Cohesion: 0.07
Nodes (36): ELSFM Android Build Skill, HeadsetEventMonitor, LocalRecommendationEngine, Media3PlayerController, PlaybackService (Media3), RecentTracksStore, ShakeDetector, ELSFM API Endpoints Reference (+28 more)

### Community 15 - "PlaylistApi"
Cohesion: 0.11
Nodes (13): CreatePlaylistRequest, ApiResult, PaginatedTracks, PaginatedTracksResponse, PlaylistApi, PlaylistInfo, PlaylistInfoDetail, PlaylistInfoPagination (+5 more)

### Community 16 - "DownloadedTrack"
Cohesion: 0.09
Nodes (8): DownloadedTrackDao, Flow, DownloadedTrack, DownloadedTrackDaoTest, FakeDownloadedTrackDao, Flow, FakeDownloadedTrackDao, Flow

### Community 17 - "ApiResult"
Cohesion: 0.10
Nodes (10): Album, Artist, Track, LikeablesRequest, ApiResult, T, NetworkError, Success (+2 more)

### Community 18 - "PlaylistViewModel"
Cohesion: 0.13
Nodes (4): StateFlow, Track, ViewModel, PlaylistViewModel

### Community 19 - "AlbumViewModel"
Cohesion: 0.05
Nodes (28): android, Modifier, LikeButton(), Modifier, Track, TrackListSection(), AlbumScreenTest, AlbumAddToPlaylistSheet() (+20 more)

### Community 20 - "ListeningHistoryViewModel"
Cohesion: 0.11
Nodes (14): AddToPlaylistSheet(), HistoryTrackRow(), com, Modifier, Track, ListeningHistoryContent(), ListeningHistoryHeader(), ListeningHistoryScreen() (+6 more)

### Community 21 - "DownloadRepository"
Cohesion: 0.11
Nodes (7): DownloadRepository, Flow, DownloadsViewModelTest, FakeDownloadedTrackDao, fakeDownloadManager(), Flow, TestDispatcherProvider

### Community 22 - "ThemeViewModel"
Cohesion: 0.10
Nodes (9): ProfileModule, SharedPreferences, ThemePreferences, ThemeStore, StateFlow, ViewModel, ThemeViewModel, FakeThemeStore (+1 more)

### Community 23 - "DiscoveryViewModel"
Cohesion: 0.05
Nodes (27): Album, Channel, CompletableDeferred, DiscoveryCacheDao, DiscoveryCache, DiscoveryCacheDaoTest, DiscoverySections, DiscoveryScreenTest (+19 more)

### Community 24 - "ArtistDetailScreen.kt"
Cohesion: 0.10
Nodes (35): ActionCallback, ActionParameters, ElsfmArtworkWidgetReceiver, ElsfmCompactWidgetReceiver, ElsfmWideWidgetReceiver, Context, GlanceAppWidget, GlanceId (+27 more)

### Community 25 - "SearchResult"
Cohesion: 0.10
Nodes (16): ArtistResult, ChannelResult, PlaylistResult, SearchResult, SearchResultSerializer, TrackResult, SearchApi, SearchResponse (+8 more)

### Community 26 - "AuthRepository"
Cohesion: 0.29
Nodes (3): AuthRepository, ApiResult, User

### Community 27 - "Media3PlayerController"
Cohesion: 0.12
Nodes (4): StateFlow, Track, Media3PlayerController, MediaController

### Community 28 - "FakeAuthApi"
Cohesion: 0.19
Nodes (7): SharedFlow, SessionManager, AuthRepositoryTest, FakeAuthApi, FakeGoogleSignInService, FakeTokenStore, FakeUserDao

### Community 29 - "FakeUserDao"
Cohesion: 0.20
Nodes (6): FakeDispatcherProvider, FakeProfileApi, FakeProfileCacheDao, FakeUserDao, CoroutineDispatcher, ProfileViewModelTest

### Community 30 - "ArtistApi.kt"
Cohesion: 0.10
Nodes (16): ArtistAlbumsPage, ArtistAlbumsPagination, ArtistAlbumsResponse, ArtistApi, ArtistFollowersPagination, ArtistFollowersResponse, ArtistResponse, ArtistTracksPagination (+8 more)

### Community 31 - "SessionManager"
Cohesion: 0.22
Nodes (3): AuthPluginTest, FakeTokenStore, SessionManagerTest

### Community 32 - "LibraryScreen.kt"
Cohesion: 0.27
Nodes (17): androidx, ChannelCard(), CreatePlaylistDialog(), emptyMessage(), Modifier, label(), LibraryContent(), LibraryEmpty() (+9 more)

### Community 34 - "DownloadsViewModel"
Cohesion: 0.20
Nodes (6): DownloadedAlbumUI, DownloadedPlaylistUI, DownloadedTrackUI, DownloadsState, DownloadsViewModel, ViewModel

### Community 35 - "ArtistCard"
Cohesion: 0.17
Nodes (5): BlurredBackground(), Modifier, Modifier, SectionHeader(), ComposablesTest

### Community 36 - "PlayerMenuEvent"
Cohesion: 0.11
Nodes (15): AddToLibrary, AddToPlaylist, AddToQueue, HideMenu, HidePlaylistPicker, MakeAvailableOffline, PlayerMenuEvent, PlayerMenuState (+7 more)

### Community 37 - "UserEntity"
Cohesion: 0.12
Nodes (4): UserDaoTest, UserEntity, ResetFakeUserDao, SignupFakeUserDao

### Community 38 - "RepostApi"
Cohesion: 0.17
Nodes (3): PlayerMenuRepository, PlayerMenuRepositoryTest, Json

### Community 39 - "PlayerController"
Cohesion: 0.12
Nodes (3): StateFlow, Track, PlayerController

### Community 40 - "FakeAccountApi"
Cohesion: 0.28
Nodes (5): AccountFakeDispatcherProvider, AccountViewModelTest, FakeAccountApi, FakeSessionsApi, CoroutineDispatcher

### Community 41 - ".viewModel"
Cohesion: 0.06
Nodes (19): GoogleSignInService, GoogleSignInServiceLike, AuthModule, SignupScreen(), AcceptPrivacyChanged, AcceptTermsChanged, ConfirmPasswordChanged, EmailChanged (+11 more)

### Community 42 - "FakePlayerController"
Cohesion: 0.12
Nodes (3): FakePlayerController, com, StateFlow

### Community 43 - "SettingsScreen"
Cohesion: 0.43
Nodes (5): SettingsScreen(), SettingsSectionHeader(), formatBytes(), SettingsSectionHeader(), StorageSection()

### Community 44 - "AccountApi"
Cohesion: 0.18
Nodes (8): AccountApi, ChangePasswordRequest, FileEntryResponse, UpdateAccountDetailsRequest, UserResponse, AccountApiTest, HttpClient, HttpStatusCode

### Community 45 - "TokenStore"
Cohesion: 0.12
Nodes (7): EncryptedTokenStore, SharedPreferences, TokenStore, bindAuthApi(), bindTokenStore(), HttpClient, NetworkModule

### Community 46 - "LoginFakeAuthApi"
Cohesion: 0.21
Nodes (5): LoginFakeAuthApi, LoginFakeGoogleSignInService, LoginFakeTokenStore, LoginFakeUserDao, LoginViewModelTest

### Community 47 - "FakePlayerController"
Cohesion: 0.13
Nodes (3): FakePlayerController, StateFlow, Track

### Community 48 - "Intent"
Cohesion: 0.12
Nodes (4): StateFlow, Track, ViewModel, PlayerViewModel

### Community 49 - ".clientReturning"
Cohesion: 0.18
Nodes (8): Track, RelatedTracksPagination, RelatedTracksResponse, TrackApi, TrackDetailResponse, HttpClient, HttpStatusCode, TrackApiTest

### Community 50 - "LoginEvent"
Cohesion: 0.14
Nodes (11): LoginScreen(), EmailChanged, GoogleSignInFailed, GoogleSignInSucceeded, ViewModel, LoginClicked, LoginEvent, LoginState (+3 more)

### Community 51 - "LibraryApiRepositoryImpl"
Cohesion: 0.15
Nodes (6): Playlist, LibraryApiRepository, Playlist, LibraryApiRepositoryImpl, LibraryData, LibraryModule

### Community 52 - "AuthApiLike"
Cohesion: 0.36
Nodes (3): AuthApiLike, ApiResult, User

### Community 53 - "User"
Cohesion: 0.20
Nodes (4): PasswordResetViewModelTest, ResetFakeAuthApi, ResetFakeGoogleSignInService, ResetFakeTokenStore

### Community 54 - "LikedSongsViewModel"
Cohesion: 0.11
Nodes (14): AddToPlaylistSheet(), com, Modifier, Track, LikedSongsContent(), LikedSongsHeader(), LikedSongsScreen(), LikedSongTrackRow() (+6 more)

### Community 55 - "SettingsViewModel"
Cohesion: 0.13
Nodes (4): StateFlow, ViewModel, SettingsState, SettingsViewModel

### Community 56 - "PlaybackService"
Cohesion: 0.20
Nodes (8): PlaybackService, Equalizer, ExoPlayer, KeyguardManager, LoudnessEnhancer, MediaSession, MediaSessionService, Player

### Community 57 - "ProfileScreen"
Cohesion: 0.32
Nodes (5): AccountDetailsPanel(), ProfileHeader(), StatItem(), ErrorScreen(), ProfileScreen()

### Community 58 - "ProfileApi"
Cohesion: 0.26
Nodes (5): Track, ProfileApi, HttpClient, HttpStatusCode, ProfileApiTest

### Community 59 - "SignupEvent"
Cohesion: 0.16
Nodes (16): Modifier, SearchContent(), SearchEmptyMessage(), SearchScreen(), SearchTab, ALBUMS, ARTISTS, PLAYLISTS (+8 more)

### Community 60 - "ChannelDetailViewModel"
Cohesion: 0.19
Nodes (6): ChannelDetailUiState, ChannelDetailViewModel, StateFlow, ViewModel, ChannelDetailViewModelTest, HttpStatusCode

### Community 61 - "DiscoveryContent"
Cohesion: 0.24
Nodes (7): ConnectivityBanner(), Modifier, Modifier, OfflineBanner(), AccountSection(), EditProfileForm(), Spacer()

### Community 62 - "ChangePasswordViewModel"
Cohesion: 0.21
Nodes (9): ChangePasswordScreen(), Modifier, PasswordField(), PasswordUpdateForm(), PasswordUpdateSuccess(), ChangePasswordState, ChangePasswordViewModel, StateFlow (+1 more)

### Community 63 - "withRetry"
Cohesion: 0.23
Nodes (4): exponentialBackoff(), T, withRetry(), RetryPolicyTest

### Community 65 - ".buildRepository"
Cohesion: 0.21
Nodes (3): FakeUserDao, HttpStatusCode, LibraryApiRepositoryImplTest

### Community 66 - "AuthApi / AuthApiLike (login endpoint client)"
Cohesion: 0.17
Nodes (13): MediaSession-Based QA Test Pattern (navigator.mediaSession checks), AuthApi / AuthApiLike (login endpoint client), AuthPlugin (Ktor: Bearer injection + 401 handling), AuthRepository (login + logout + session restore), DispatcherProvider Interface (core:common), SessionManager (token save/clear + Expired events), TokenStore / EncryptedTokenStore (EncryptedSharedPreferences), MediaItemFactory (Track to MediaItem mapping) (+5 more)

### Community 67 - "ElsfmNavHost"
Cohesion: 0.07
Nodes (35): BottomTab, ElsfmNavHost(), navigateToAlbum(), navigateToPlaylist(), navigateToTrackComments(), navigateToUserProfile(), Album, Artist (+27 more)

### Community 68 - "AlbumCard"
Cohesion: 0.25
Nodes (6): Album, Modifier, NewReleasesSection(), AlbumCard(), Album, Modifier

### Community 69 - "MediaItemFactoryTest"
Cohesion: 0.23
Nodes (6): MediaItemFactoryTest, Artist, ArtistLink, ArtistProfile, Track, TrackAlbum

### Community 70 - "UserApi.kt"
Cohesion: 0.17
Nodes (11): FollowedUsersPagination, FollowedUsersResponse, FollowersPagination, FollowersResponse, LikeableRequestItem, LikedAlbumsPagination, LikedAlbumsResponse, LikedArtistsPagination (+3 more)

### Community 71 - "ChannelDetailScreen.kt"
Cohesion: 0.32
Nodes (12): AlbumGridContent(), ChannelDetailBody(), ChannelDetailError(), ChannelDetailLoading(), ChannelDetailScreen(), Album, Modifier, Playlist (+4 more)

### Community 72 - ".viewModel"
Cohesion: 0.19
Nodes (7): FakeLibraryApiRepository, Playlist, FakeDispatcherProvider, FakeNetworkMonitor, CoroutineDispatcher, Flow, LibraryViewModelTest

### Community 74 - "ProfileViewModel"
Cohesion: 0.22
Nodes (4): ProfileState, StateFlow, ViewModel, ProfileViewModel

### Community 75 - "StartDestinationViewModel"
Cohesion: 0.18
Nodes (9): SharedFlow, StateFlow, ViewModel, Loading, Resolved, StartDestinationState, StartDestinationViewModel, Expired (+1 more)

### Community 76 - "LibraryCacheDao"
Cohesion: 0.21
Nodes (3): LibraryCacheDao, LibraryCache, FakeLibraryCacheDao

### Community 77 - "PlayHistoryApi"
Cohesion: 0.23
Nodes (5): PlayHistoryApi, RecordPlayRequest, HttpClient, HttpStatusCode, PlayHistoryApiTest

### Community 78 - ".clientReturning"
Cohesion: 0.21
Nodes (6): AlbumApi, AlbumResponse, Album, AlbumApiTest, HttpClient, HttpStatusCode

### Community 79 - "ChannelApi"
Cohesion: 0.33
Nodes (4): ChannelApi, ChannelApiTest, HttpClient, HttpStatusCode

### Community 80 - ".clientReturning"
Cohesion: 0.27
Nodes (5): SessionsApi, UserSessionsResponse, HttpClient, HttpStatusCode, SessionsApiTest

### Community 82 - "EmailVerificationEvent"
Cohesion: 0.20
Nodes (8): EmailVerificationScreen(), CodeChanged, EmailSet, EmailVerificationEvent, EmailVerificationState, EmailVerificationViewModel, ViewModel, VerifyClicked

### Community 83 - "DownloadsEvent"
Cohesion: 0.17
Nodes (12): DeleteDownload, DownloadLibrary, DownloadsEvent, PlayAlbum, PlayAll, PlayPlaylist, PlayTrack, SearchQueryChanged (+4 more)

### Community 84 - "SleepTimer"
Cohesion: 0.24
Nodes (3): SleepTimer, SleepTimerTest, Job

### Community 85 - "AccountViewModel"
Cohesion: 0.21
Nodes (5): AccountState, AccountViewModel, ByteArray, StateFlow, ViewModel

### Community 87 - "DownloadQuality"
Cohesion: 0.20
Nodes (8): toMediaItem(), DownloadQuality, HIGH, LOW, MEDIUM, SharedPreferences, SessionPreferences, MediaItem

### Community 91 - "FakeUserDao"
Cohesion: 0.30
Nodes (14): ArtworkImage(), ArtworkLayout(), CompactLayout(), ElsfmWidget, Context, GlanceAppWidget, GlanceId, PlayPauseButton() (+6 more)

### Community 92 - "ElsfmApplication"
Cohesion: 0.22
Nodes (6): ElsfmApplication, Application, Configuration, HiltWorkerFactory, ImageLoader, ImageLoaderFactory

### Community 93 - "Phase 1 Foundation Implementation Plan"
Cohesion: 0.20
Nodes (10): CLAUDE.md Coding Principles, Phase 1 Foundation Implementation Plan, Phase 2 Player Implementation Plan, Phase 3 Library Implementation Plan, Phase 4 Advanced Features Implementation Plan, Phase 5 Navigation & UI Redesign Plan, Phase 1 Foundation Design Spec, Phase 2 Player Design Spec (+2 more)

### Community 95 - "Channel"
Cohesion: 0.24
Nodes (6): Channel, FeaturedPlaylistCard(), FeaturedPlaylistsSection(), Modifier, ChannelListComposable(), Modifier

### Community 96 - "DiscoveryScreenTest"
Cohesion: 0.19
Nodes (3): FakeUserApi, FollowStateRepositoryTest, FakeUserApi

### Community 98 - ".downSwipeTriggersOnExpandClicked"
Cohesion: 0.49
Nodes (3): HttpClient, MiniPlayerTest, MiniPlayer()

### Community 99 - "PlayerScreen"
Cohesion: 0.42
Nodes (9): AddToPlaylistBottomSheet(), formatDuration(), Modifier, Track, PlayerScreen(), QueueBottomSheet(), QueueRow(), SleepTimerDialog() (+1 more)

### Community 100 - "ELSFM Native App Feature Inventory"
Cohesion: 0.22
Nodes (9): ELSFM QA Skill, ELSFM Ship / Release Skill, Build ELSFM Native Android CI Workflow, Build and Release APK CI Workflow, BeMusic Laravel Backend API Integration (elsfm.com), ELSFM Native App Feature Inventory, Media3/ExoPlayer Background Playback via PlaybackService, ELSFM Modular Gradle Architecture (app + feature:* + core:*) (+1 more)

### Community 101 - "ElsfmTheme"
Cohesion: 0.25
Nodes (3): ElsfmThemeTest, ElsfmTheme(), LoginScreenTest

### Community 102 - "ShakeDetector"
Cohesion: 0.22
Nodes (4): ShakeDetector, Sensor, SensorEvent, SensorEventListener

### Community 103 - "ShakeSensitivity"
Cohesion: 0.22
Nodes (6): SharedPreferences, ShakePreferences, ShakeSensitivity, HIGH, LOW, MEDIUM

### Community 104 - "ProfileApi.kt"
Cohesion: 0.20
Nodes (9): UserProfile, RecentlyPlayedPagination, RecentlyPlayedResponse, toUserProfile(), UpdateProfileRequest, UpdateProfileUser, UserProfileDetailsDto, UserProfilePageDto (+1 more)

### Community 105 - "PlaylistCard"
Cohesion: 0.18
Nodes (9): FeaturedPlaylistsSection(), Modifier, Playlist, Modifier, Playlist, PlaylistCard(), Modifier, PlaylistsShimmer() (+1 more)

### Community 106 - ".uploadAvatar"
Cohesion: 0.33
Nodes (3): FileEntry, ByteArray, ByteArray

### Community 107 - "ImageUrlSerializer"
Cohesion: 0.25
Nodes (5): ImageUrlSerializer, Decoder, Encoder, KSerializer, SerialDescriptor

### Community 108 - "NullSafeLongSerializer"
Cohesion: 0.25
Nodes (5): Decoder, Encoder, KSerializer, SerialDescriptor, NullSafeLongSerializer

### Community 109 - "ChannelApi.kt"
Cohesion: 0.25
Nodes (7): ChannelContentConfig, ChannelContentDetail, ChannelContentPagination, ChannelContentPaginationRaw, ChannelContentShowResponse, ChannelDetail, ChannelShowResponse

### Community 110 - "LoginUiState"
Cohesion: 0.25
Nodes (7): FieldErrors, Idle, InvalidCredentials, Loading, LoginUiState, NetworkError, Success

### Community 111 - "RecentlyPlayedSection"
Cohesion: 0.25
Nodes (6): Modifier, Track, RecentlyPlayedSection(), Modifier, Track, TrackCard()

### Community 112 - "PlayerState"
Cohesion: 0.25
Nodes (5): PlayerRepeatMode, ALL, OFF, ONE, PlayerState

### Community 113 - ".newApplication"
Cohesion: 0.29
Nodes (5): AndroidJUnitRunner, HiltTestRunner, Application, Context, ClassLoader

### Community 114 - "HomeViewModel"
Cohesion: 0.13
Nodes (6): HomePlaceholderScreen(), HomeViewModel, StateFlow, Track, ViewModel, User

### Community 115 - "TrackListItem"
Cohesion: 0.16
Nodes (9): PasswordResetScreen(), EmailChanged, PasswordChanged, PasswordConfirmChanged, PasswordResetEvent, PasswordResetState, PasswordResetViewModel, ResetClicked (+1 more)

### Community 116 - ".toChannelContentResult"
Cohesion: 0.52
Nodes (5): Albums, ChannelContentResult, Channels, Playlists, Tracks

### Community 117 - "Track (Eloquent model)"
Cohesion: 0.40
Nodes (6): Album (Eloquent model), Artist (Eloquent model), toNormalizedArray (polymorphic normalization convention), Track (Eloquent model), PaginatedResult<T> (Dart generic), Track (Dart model with fromJson)

### Community 118 - "Spacer"
Cohesion: 0.60
Nodes (5): CommentInputBar(), CommentRow(), CommentsErrorState(), CommentsScreen(), formatCommentDate()

### Community 119 - "PopularSongRow"
Cohesion: 0.53
Nodes (5): formatTrackDuration(), Modifier, Track, PopularSongRow(), PopularSongsSection()

### Community 120 - "DownloadsScreen"
Cohesion: 0.60
Nodes (5): DownloadedGroupCard(), DownloadLibraryBanner(), DownloadsScreen(), EmptyDownloads(), TrackDownloadItem()

### Community 121 - "DeepLinkTrackViewModel"
Cohesion: 0.40
Nodes (3): DeepLinkTrackViewModel, Track, ViewModel

### Community 122 - "AutoCacheWorker"
Cohesion: 0.40
Nodes (3): AutoCacheWorker, Result, CoroutineWorker

### Community 127 - "RecentlyPlayedTrackItem"
Cohesion: 0.70
Nodes (4): formatDuration(), Track, RecentlyPlayedSection(), RecentlyPlayedTrackItem()

### Community 128 - "Flutter-Laravel Integration Skill"
Cohesion: 0.50
Nodes (4): Flutter API Error Handling Reference, Flutter ApiClient Example Reference, Flutter Response Mapping Reference, Flutter-Laravel Integration Skill

### Community 132 - "ChannelApi + SearchApi + ArtistApi (Phase 3 network layer)"
Cohesion: 0.50
Nodes (4): ChannelApi + SearchApi + ArtistApi (Phase 3 network layer), LibraryViewModel (channel + playlist browse state), SearchViewModel (mixed-result search state), DiscoveryViewModel (trending + recommendations state)

### Community 133 - "DownloadTab"
Cohesion: 0.50
Nodes (4): DownloadTab, ALBUMS, PLAYLISTS, SONGS

### Community 134 - "ELSFM Capacitor Build Skill"
Cohesion: 0.67
Nodes (3): ELSFM Capacitor Build Skill, ElsfmBridge (JavascriptInterface), ForegroundAudioService (Android Java)

### Community 135 - "ELSFM Auth Hero Image (Hands Playing Harp, Nepali Text: Songs of Zion)"
Cohesion: 0.67
Nodes (3): ELSFM Auth Hero Image (Hands Playing Harp, Nepali Text: Songs of Zion), ELSFM App Launcher Icon (Harp/Songs of Zion Branding), Feature Auth Module Hero Image (Harp Playing, Songs of Zion)

### Community 142 - "Material 3 Full Design System Integration (Phase 5 redesign)"
Cohesion: 0.67
Nodes (3): ElsfmTheme Compose Design System (dark/light color scheme), BlurredBackground Composable (reusable artwork backdrop), Material 3 Full Design System Integration (Phase 5 redesign)

### Community 146 - "UserSessionInfo"
Cohesion: 0.38
Nodes (3): UserSessionInfo, SessionRow(), SessionsPanel()

### Community 152 - "Playlist.kt"
Cohesion: 0.20
Nodes (3): FakePlayerController, StateFlow, Track

### Community 190 - "TrackListApi"
Cohesion: 0.21
Nodes (7): PlaylistTracksPage, PlaylistTracksPagination, PlaylistTracksResponse, TrackListApi, HttpClient, HttpStatusCode, TrackListApiTest

### Community 191 - "elsfmJson"
Cohesion: 0.22
Nodes (3): elsfmJson(), HttpClient, ElsfmJsonTest

### Community 193 - ".paginatedViewModel"
Cohesion: 0.19
Nodes (3): TrackLikeController, FakeDispatcherProvider, CoroutineDispatcher

### Community 194 - "DefaultNetworkMonitor"
Cohesion: 0.23
Nodes (7): DefaultNetworkMonitor, Flow, NetworkMonitor, bindNetworkMonitor(), Context, NetworkMonitorTest, ShadowConnectivityManager

### Community 195 - "DispatcherProvider"
Cohesion: 0.25
Nodes (7): DefaultDispatcherProvider, DispatcherProvider, CoroutineDispatcher, DispatcherProviderTest, bindDispatcherProvider(), fakeDownloadRepository(), NoopDispatcherProvider

### Community 196 - "fakeDownloadRepository"
Cohesion: 0.22
Nodes (6): DownloadManager, Result, Track, slugify(), fakeDownloadRepository(), NoopDispatcherProvider

### Community 197 - "ArtistCard"
Cohesion: 0.28
Nodes (6): ArtistCard(), Artist, Modifier, ArtistsShimmer(), ArtistsTabContent(), Modifier

### Community 198 - "SubscriptionScreen.kt"
Cohesion: 0.39
Nodes (8): dateLabel(), FreeTierContent(), Context, openBillingPage(), statusLabel(), SubscriptionContent(), SubscriptionErrorContent(), SubscriptionScreen()

### Community 199 - "RepostApi"
Cohesion: 0.57
Nodes (3): RepostApi, RepostToggleRequest, RepostToggleResponse

### Community 202 - "SortBy"
Cohesion: 0.50
Nodes (4): SortBy, A_TO_Z, RECENTLY_ADDED, RELEASE_DATE

## Knowledge Gaps
- **210 isolated node(s):** `BottomTab`, `ArtistTracksPagination`, `ArtistTracksResponse`, `ArtistAlbumsPagination`, `ArtistAlbumsResponse` (+205 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **56 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ApiResult` connect `ApiResult` to `SubscriptionViewModel`, `NotificationsViewModel`, `FakePlayerController`, `CommentsViewModel`, `UserProfileViewModel`, `PlaylistApi`, `UserSessionInfo`, `UserProfile`, `SearchResult`, `FakeAuthApi`, `FakeUserDao`, `RepostApi`, `FakeAccountApi`, `.viewModel`, `AccountApi`, `LoginFakeAuthApi`, `.clientReturning`, `LibraryApiRepositoryImpl`, `User`, `ProfileApi`, `TrackListApi`, `withRetry`, `RepostApi`, `.viewModel`, `UserApiLike`, `PlayHistoryApi`, `.clientReturning`, `AccountViewModel`, `Channel`, `DiscoveryScreenTest`, `ProfileApi.kt`, `.uploadAvatar`, `HomeViewModel`, `.toChannelContentResult`?**
  _High betweenness centrality (0.177) - this node is a cross-community bridge._
- **Why does `elsfmJson()` connect `elsfmJson` to `PlaylistViewModelTest`, `SearchUiState`, `NotificationsViewModel`, `LibraryViewModel`, `UserApi`, `AuthApi`, `PlaylistApi`, `DiscoveryViewModel`, `SearchResult`, `ArtistApi.kt`, `RepostApi`, `AccountApi`, `TokenStore`, `.clientReturning`, `ChannelDetailViewModel`, `TrackListApi`, `elsfmJson`, `.buildRepository`, `ListeningHistoryViewModelTest`, `.paginatedViewModel`, `.controllerReturning`, `.viewModel`, `ProfileViewModel`, `.clientReturning`, `ChannelApi`, `.clientReturning`, `.toChannelContentResult`?**
  _High betweenness centrality (0.160) - this node is a cross-community bridge._
- **Why does `UserApi` connect `UserApi` to `elsfmJson`, `.buildRepository`, `ArtistDetailViewModel`, `ListeningHistoryViewModelTest`, `.paginatedViewModel`, `.downSwipeTriggersOnExpandClicked`, `UserApi.kt`, `RepostApi`, `.controllerReturning`, `UserApiLike`, `.viewModel`, `PlayerViewModel`, `SearchUiState`, `UserProfileViewModel`, `ApiResult`, `elsfmJson`?**
  _High betweenness centrality (0.126) - this node is a cross-community bridge._
- **Are the 39 inferred relationships involving `UserApi` (e.g. with `.`addTrackToLibrary posts likeables payload to add-to-library endpoint`()` and `.`addTrackToLibrary returns NetworkError on exception`()`) actually correct?**
  _`UserApi` has 39 INFERRED edges - model-reasoned connections that need verification._
- **Are the 51 inferred relationships involving `elsfmJson()` (e.g. with `.toChannelContentResult()` and `.provideHttpClient()`) actually correct?**
  _`elsfmJson()` has 51 INFERRED edges - model-reasoned connections that need verification._
- **Are the 14 inferred relationships involving `PlayerViewModel` (e.g. with `.downSwipeTriggersOnExpandClicked()` and `.leftSwipeTriggersSkipNext()`) actually correct?**
  _`PlayerViewModel` has 14 INFERRED edges - model-reasoned connections that need verification._
- **What connects `BottomTab`, `ArtistTracksPagination`, `ArtistTracksResponse` to the rest of the system?**
  _210 weakly-connected nodes found - possible documentation gaps or missing edges._