# ELSFM Native — Phase 1 (Foundation) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a new, modular native Android (Kotlin) project for elsfm.com with a working, tested, synced login flow (Sanctum bearer token via the real mobile API), encrypted session storage, OS-level saved-password support, and centralized session-expiry handling — the foundation later phases (player, library, etc.) build on.

**Architecture:** Modular Gradle project (`app` + `core:*` + `feature:*`), Jetpack Compose UI, Hilt DI, Ktor client against `https://www.elsfm.com/api/v1/...`, Room for local profile cache, `EncryptedSharedPreferences` for the auth token only.

**Tech Stack:** Kotlin 2.0.21, AGP 8.6.1, Jetpack Compose (BOM 2024.10.00), Ktor 3.0.1 (OkHttp engine), kotlinx.serialization 1.7.3, Room 2.6.1, Hilt 2.52 + KSP, Turbine 1.2.0, JUnit4.

## Global Constraints

- Base package: `com.elsfm.mobile`. Application ID: `com.elsfm.mobile`.
- `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`, Java/Kotlin JVM target 17.
- `core:*` modules must never depend on `feature:*` or `app` modules (one-way dependency graph: `app` → `feature:*` → `core:*`).
- The Sanctum access token is stored **only** in `EncryptedSharedPreferences` — never in Room, plain `SharedPreferences`, logs, or committed files.
- No automated test may hit the real `elsfm.com` API or use real account credentials — all network behavior in tests is exercised via Ktor `MockEngine` or fakes. The one real end-to-end login check is a manual step the project owner performs themselves (see Task 15).
- New repo: local folder `/Users/siku/Documents/GitHub/elsfm-native` (already `git init`'d with the design spec committed), pushed to `github.com/dewanlabung/elsfm-native` (private). Never touch `/Users/siku/Documents/GitHub/elsfm_flutter`.
- Every module's Gradle dependency versions come from the single version catalog `gradle/libs.versions.toml` — no inline version strings in module `build.gradle.kts` files.

---

### Task 1: Project scaffold — root Gradle config + blank app module

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/elsfm/mobile/ElsfmApplication.kt`
- Create: `app/src/main/kotlin/com/elsfm/mobile/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `.gitignore`

**Interfaces:**
- Produces: a buildable Gradle project with module coordinates `:app`, `:core:model`, `:core:network`, `:core:database`, `:core:designsystem`, `:core:common`, `:feature:auth` (declared in `settings.gradle.kts`, most not yet containing code — added in later tasks).

- [ ] **Step 1: Write the version catalog**

`gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.6.1"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
coreKtx = "1.13.1"
lifecycle = "2.8.6"
activityCompose = "1.9.3"
composeBom = "2024.10.00"
navigationCompose = "2.8.3"
hilt = "2.52"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
ktor = "3.0.1"
kotlinxSerializationJson = "1.7.3"
coroutines = "1.9.0"
securityCrypto = "1.1.0-alpha06"
credentials = "1.3.0"
junit = "4.13.2"
androidxTestExt = "1.2.1"
espresso = "3.6.1"
turbine = "1.2.0"
hiltAndroidTesting = "2.52"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hiltAndroidTesting" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { group = "io.ktor", name = "ktor-client-logging", version.ref = "ktor" }
ktor-client-mock = { group = "io.ktor", name = "ktor-client-mock", version.ref = "ktor" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
androidx-credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
androidx-credentials-play-services-auth = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExt" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room", version.ref = "room" }
```

- [ ] **Step 2: Write root Gradle files**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "elsfm-native"

include(":app")
include(":core:model")
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:designsystem")
include(":feature:auth")
```

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

`.gitignore`:
```
*.iml
.gradle/
/local.properties
/.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
*.apk
*.keystore.jks
```

- [ ] **Step 3: Write the app module**

`app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.elsfm.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.elsfm.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:auth"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/auto">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".ElsfmApplication"
        android:allowBackup="false"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:theme="@style/Theme.Elsfm">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Elsfm">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">ELSFM</string>
</resources>
```

`app/src/main/res/values/styles.xml`:
```xml
<resources>
    <style name="Theme.Elsfm" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`app/src/main/kotlin/com/elsfm/mobile/ElsfmApplication.kt`:
```kotlin
package com.elsfm.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ElsfmApplication : Application()
```

`app/src/main/kotlin/com/elsfm/mobile/MainActivity.kt`:
```kotlin
package com.elsfm.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    Text("ELSFM")
                }
            }
        }
    }
}
```

- [ ] **Step 4: Bootstrap the Gradle wrapper**

There is no system-installed `gradle` binary on this machine, so bootstrap the wrapper using a temporary downloaded Gradle distribution (only used once, not committed):

Run:
```bash
cd /Users/siku/Documents/GitHub/elsfm-native
curl -sL -o /tmp/gradle-8.9-bin.zip https://services.gradle.org/distributions/gradle-8.9-bin.zip
unzip -q -o /tmp/gradle-8.9-bin.zip -d /tmp/gradle-bootstrap
/tmp/gradle-bootstrap/gradle-8.9/bin/gradle wrapper --gradle-version 8.9 --distribution-type bin
rm -rf /tmp/gradle-8.9-bin.zip /tmp/gradle-bootstrap
```
Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, and `gradle/wrapper/gradle-wrapper.jar` now exist in the project root.

- [ ] **Step 5: Verify the blank app builds**

Run:
```bash
cd /Users/siku/Documents/GitHub/elsfm-native
export ANDROID_HOME="$HOME/Library/Android/sdk"
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug --console=plain
```
Expected: `BUILD SUCCESSFUL`. (This will fail on missing `:core:*`/`:feature:auth` project directories until they contain at least a `build.gradle.kts` — if it fails with "project not found," temporarily comment out the `implementation(project(":core:..."))` lines in `app/build.gradle.kts` and the corresponding `include(...)` lines in `settings.gradle.kts` for this one verification build, then restore them immediately after — later tasks will create those modules for real.)

- [ ] **Step 6: Commit**

```bash
cd /Users/siku/Documents/GitHub/elsfm-native
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/ gradlew gradlew.bat .gitignore app/ local.properties
git rm --cached local.properties 2>/dev/null; echo "/local.properties" >> .gitignore; git add .gitignore
git commit -m "chore: scaffold Gradle project with blank app module"
```

---

### Task 2: core:common — DispatcherProvider

**Files:**
- Create: `core/common/build.gradle.kts`
- Create: `core/common/src/main/kotlin/com/elsfm/mobile/core/common/DispatcherProvider.kt`
- Test: `core/common/src/test/kotlin/com/elsfm/mobile/core/common/DispatcherProviderTest.kt`

**Interfaces:**
- Produces: `interface DispatcherProvider { val io: CoroutineDispatcher; val main: CoroutineDispatcher; val default: CoroutineDispatcher }`, `class DefaultDispatcherProvider : DispatcherProvider`. Consumed by `core:network` (Task 5) and `feature:auth` (Task 12).

- [ ] **Step 1: Write the failing test**

`core/common/src/test/kotlin/com/elsfm/mobile/core/common/DispatcherProviderTest.kt`:
```kotlin
package com.elsfm.mobile.core.common

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DispatcherProviderTest {
    @Test
    fun `default provider exposes real coroutine dispatchers`() {
        val provider: DispatcherProvider = DefaultDispatcherProvider()

        assertEquals(Dispatchers.IO, provider.io)
        assertEquals(Dispatchers.Main, provider.main)
        assertEquals(Dispatchers.Default, provider.default)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:common:test --console=plain`
Expected: FAIL — `core/common` module and `DispatcherProvider` do not exist yet.

- [ ] **Step 3: Write the module and implementation**

`core/common/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

`core/common/src/main/kotlin/com/elsfm/mobile/core/common/DispatcherProvider.kt`:
```kotlin
package com.elsfm.mobile.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

interface DispatcherProvider {
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val default: CoroutineDispatcher = Dispatchers.Default
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:common:test --console=plain`
Expected: `BUILD SUCCESSFUL`, 1 test passed.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts core/common
git commit -m "feat(core-common): add DispatcherProvider"
```

---

### Task 3: core:model — auth data classes

**Files:**
- Create: `core/model/build.gradle.kts`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/Permission.kt`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/User.kt`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/LoginRequest.kt`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/LoginResponse.kt`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/LaravelValidationError.kt`
- Test: `core/model/src/test/kotlin/com/elsfm/mobile/core/model/LoginResponseSerializationTest.kt`

**Interfaces:**
- Produces: `User(id: Int, username: String?, name: String?, email: String, avatarUrl: String?, permissions: List<Permission>, accessToken: String?)`, `Permission(id: Int, name: String)`, `LoginRequest(email: String, password: String, tokenName: String)`, `LoginResponse(status: String, user: User)`, `LaravelValidationError(message: String, errors: Map<String, List<String>>)`. Consumed by `core:network` (Tasks 4, 6, 7) and `feature:auth` (Tasks 11, 12).

- [ ] **Step 1: Write the failing test**

`core/model/src/test/kotlin/com/elsfm/mobile/core/model/LoginResponseSerializationTest.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginResponseSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes real backend login response shape`() {
        val body = """
            {
              "status": "success",
              "user": {
                "id": 207,
                "username": null,
                "name": "ELSFM APP",
                "email": "test.elsfm@gmail.com",
                "avatar_url": null,
                "permissions": [
                  {"id": 49, "name": "music.view", "restrictions": []},
                  {"id": 52, "name": "music.embed", "restrictions": []}
                ],
                "access_token": "1|abcdef1234567890"
              },
              "themes": {"light": {}, "dark": {}},
              "menus": [],
              "settings": {},
              "locales": []
            }
        """.trimIndent()

        val decoded = json.decodeFromString<LoginResponse>(body)

        assertEquals(207, decoded.user.id)
        assertEquals("ELSFM APP", decoded.user.name)
        assertNull(decoded.user.username)
        assertEquals(2, decoded.user.permissions.size)
        assertEquals("music.view", decoded.user.permissions.first().name)
        assertEquals("1|abcdef1234567890", decoded.user.accessToken)
    }

    @Test
    fun `decodes laravel validation error shape`() {
        val body = """
            {"message": "The given data was invalid.", "errors": {"email": ["These credentials do not match our records."]}}
        """.trimIndent()

        val decoded = json.decodeFromString<LaravelValidationError>(body)

        assertEquals("The given data was invalid.", decoded.message)
        assertEquals(listOf("These credentials do not match our records."), decoded.errors["email"])
    }

    @Test
    fun `encodes login request with snake case token_name field`() {
        val request = LoginRequest(email = "a@b.com", password = "secret", tokenName = "android-uuid-1")

        val encoded = json.encodeToString(LoginRequest.serializer(), request)

        assertEquals(true, encoded.contains("\"token_name\":\"android-uuid-1\""))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:model:test --console=plain`
Expected: FAIL — `core/model` module and its classes do not exist yet.

- [ ] **Step 3: Write the module and data classes**

`core/model/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
```

`core/model/src/main/kotlin/com/elsfm/mobile/core/model/Permission.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Permission(
    val id: Int,
    val name: String,
)
```

`core/model/src/main/kotlin/com/elsfm/mobile/core/model/User.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val username: String? = null,
    val name: String? = null,
    val email: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val permissions: List<Permission> = emptyList(),
    @SerialName("access_token") val accessToken: String? = null,
)
```

`core/model/src/main/kotlin/com/elsfm/mobile/core/model/LoginRequest.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("token_name") val tokenName: String,
)
```

`core/model/src/main/kotlin/com/elsfm/mobile/core/model/LoginResponse.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val status: String = "success",
    val user: User,
)
```

`core/model/src/main/kotlin/com/elsfm/mobile/core/model/LaravelValidationError.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LaravelValidationError(
    val message: String,
    val errors: Map<String, List<String>> = emptyMap(),
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:model:test --console=plain`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts core/model
git commit -m "feat(core-model): add User, Permission and login request/response models"
```

---

### Task 4: core:network — ApiResult + JSON configuration

**Files:**
- Create: `core/network/build.gradle.kts`
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/ApiResult.kt`
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/ElsfmJson.kt`
- Test: `core/network/src/test/kotlin/com/elsfm/mobile/core/network/ElsfmJsonTest.kt`

**Interfaces:**
- Produces: `sealed interface ApiResult<out T> { Success<T>(data: T); ValidationError(fields: Map<String, List<String>>); Unauthorized; NetworkError(cause: Throwable) }`, `fun elsfmJson(): Json`. Consumed by Tasks 6, 7, 8, and `feature:auth` (Task 11).

- [ ] **Step 1: Write the failing test**

`core/network/src/test/kotlin/com/elsfm/mobile/core/network/ElsfmJsonTest.kt`:
```kotlin
package com.elsfm.mobile.core.network

import com.elsfm.mobile.core.model.LoginResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class ElsfmJsonTest {
    @Test
    fun `ignores unknown top level keys from the real bootstrap payload`() {
        val json = elsfmJson()
        val body = """
            {"status":"success","user":{"id":1,"email":"a@b.com"},"themes":{},"menus":[],"settings":{},"locales":[]}
        """.trimIndent()

        val decoded = json.decodeFromString<LoginResponse>(body)

        assertEquals(1, decoded.user.id)
        assertEquals("a@b.com", decoded.user.email)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:network:test --console=plain`
Expected: FAIL — `core/network` module does not exist yet.

- [ ] **Step 3: Write the module and implementation**

`core/network/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.elsfm.mobile.core.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.androidx.security.crypto)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.turbine)
}
```

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/ApiResult.kt`:
```kotlin
package com.elsfm.mobile.core.network

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class ValidationError(val fields: Map<String, List<String>>) : ApiResult<Nothing>
    data object Unauthorized : ApiResult<Nothing>
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>
}
```

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/ElsfmJson.kt`:
```kotlin
package com.elsfm.mobile.core.network

import kotlinx.serialization.json.Json

fun elsfmJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:network:test --console=plain`
Expected: `BUILD SUCCESSFUL`, 1 test passed.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts core/network
git commit -m "feat(core-network): add ApiResult and shared Json configuration"
```

---

### Task 5: core:network — TokenStore, SessionManager, SessionEvent

**Files:**
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/auth/TokenStore.kt`
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/auth/EncryptedTokenStore.kt`
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/auth/SessionEvent.kt`
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/auth/SessionManager.kt`
- Test: `core/network/src/test/kotlin/com/elsfm/mobile/core/network/auth/SessionManagerTest.kt`

**Interfaces:**
- Consumes: `DispatcherProvider` (Task 2).
- Produces: `interface TokenStore { suspend fun save(token: String); suspend fun read(): String?; suspend fun clear() }`, `class EncryptedTokenStore(context: Context, dispatcherProvider: DispatcherProvider) : TokenStore`, `sealed interface SessionEvent { data object Expired : SessionEvent }`, `class SessionManager(tokenStore: TokenStore) { suspend fun saveToken(token: String); suspend fun currentToken(): String?; suspend fun clear(); suspend fun notifyExpired(); val events: SharedFlow<SessionEvent> }`. Consumed by Tasks 6, 8, and `feature:auth` (Task 11).

- [ ] **Step 1: Write the failing test**

`core/network/src/test/kotlin/com/elsfm/mobile/core/network/auth/SessionManagerTest.kt`:
```kotlin
package com.elsfm.mobile.core.network.auth

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeTokenStore : TokenStore {
    private var token: String? = null
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

class SessionManagerTest {
    @Test
    fun `saves and reads back a token`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())

        sessionManager.saveToken("token-123")

        assertEquals("token-123", sessionManager.currentToken())
    }

    @Test
    fun `clear removes the stored token`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        sessionManager.saveToken("token-123")

        sessionManager.clear()

        assertNull(sessionManager.currentToken())
    }

    @Test
    fun `notifyExpired clears the token and emits an Expired event`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        sessionManager.saveToken("token-123")

        sessionManager.events.test {
            sessionManager.notifyExpired()
            assertEquals(SessionEvent.Expired, awaitItem())
        }
        assertNull(sessionManager.currentToken())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:network:test --console=plain`
Expected: FAIL — `TokenStore`/`SessionManager`/`SessionEvent` do not exist yet.

- [ ] **Step 3: Write the implementation**

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/auth/TokenStore.kt`:
```kotlin
package com.elsfm.mobile.core.network.auth

interface TokenStore {
    suspend fun save(token: String)
    suspend fun read(): String?
    suspend fun clear()
}
```

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/auth/EncryptedTokenStore.kt`:
```kotlin
package com.elsfm.mobile.core.network.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.elsfm.mobile.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EncryptedTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) : TokenStore {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "elsfm_secure_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun save(token: String) = withContext(dispatcherProvider.io) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override suspend fun read(): String? = withContext(dispatcherProvider.io) {
        prefs.getString(KEY_TOKEN, null)
    }

    override suspend fun clear() = withContext(dispatcherProvider.io) {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val KEY_TOKEN = "access_token"
    }
}
```

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/auth/SessionEvent.kt`:
```kotlin
package com.elsfm.mobile.core.network.auth

sealed interface SessionEvent {
    data object Expired : SessionEvent
}
```

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/auth/SessionManager.kt`:
```kotlin
package com.elsfm.mobile.core.network.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    suspend fun saveToken(token: String) {
        tokenStore.save(token)
    }

    suspend fun currentToken(): String? = tokenStore.read()

    suspend fun clear() {
        tokenStore.clear()
    }

    suspend fun notifyExpired() {
        tokenStore.clear()
        _events.emit(SessionEvent.Expired)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:network:test --console=plain`
Expected: `BUILD SUCCESSFUL`, 3 new tests passed (4 total in the module).

- [ ] **Step 5: Commit**

```bash
git add core/network
git commit -m "feat(core-network): add SessionManager with encrypted token storage"
```

---

### Task 6: core:network — AuthPlugin (Bearer injection + 401 handling)

**Files:**
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/auth/AuthPlugin.kt`
- Test: `core/network/src/test/kotlin/com/elsfm/mobile/core/network/auth/AuthPluginTest.kt`

**Interfaces:**
- Consumes: `SessionManager` (Task 5).
- Produces: `val AuthPlugin: ClientPlugin<AuthPluginConfig>`, `class AuthPluginConfig { lateinit var sessionManager: SessionManager }`. Consumed by Task 8's `NetworkModule`.

- [ ] **Step 1: Write the failing test**

`core/network/src/test/kotlin/com/elsfm/mobile/core/network/auth/AuthPluginTest.kt`:
```kotlin
package com.elsfm.mobile.core.network.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeTokenStore(initial: String? = null) : TokenStore {
    private var token: String? = initial
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

class AuthPluginTest {

    @Test
    fun `attaches bearer token from session manager to requests`() = runTest {
        var capturedAuthHeader: String? = null
        val sessionManager = SessionManager(FakeTokenStore(initial = "secret-token"))
        val mockEngine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(mockEngine) {
            install(AuthPlugin) { this.sessionManager = sessionManager }
        }

        client.get("https://www.elsfm.com/api/v1/tracks")

        assertEquals("Bearer secret-token", capturedAuthHeader)
    }

    @Test
    fun `clears session and does not crash on 401 response`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore(initial = "stale-token"))
        val mockEngine = MockEngine { _ ->
            respond("{}", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(mockEngine) {
            install(AuthPlugin) { this.sessionManager = sessionManager }
        }

        client.get("https://www.elsfm.com/api/v1/tracks")

        assertNull(sessionManager.currentToken())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:network:test --console=plain`
Expected: FAIL — `AuthPlugin` does not exist yet.

- [ ] **Step 3: Write the implementation**

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/auth/AuthPlugin.kt`:
```kotlin
package com.elsfm.mobile.core.network.auth

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class AuthPluginConfig {
    lateinit var sessionManager: SessionManager
}

val AuthPlugin = createClientPlugin("AuthPlugin", ::AuthPluginConfig) {
    val sessionManager = pluginConfig.sessionManager

    onRequest { request, _ ->
        val path = request.url.encodedPath
        if (!path.contains("/auth/login") && !path.contains("/auth/register")) {
            sessionManager.currentToken()?.let { token ->
                request.headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    onResponse { response ->
        if (response.status == HttpStatusCode.Unauthorized) {
            sessionManager.notifyExpired()
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:network:test --console=plain`
Expected: `BUILD SUCCESSFUL`, 2 new tests passed (6 total in the module).

- [ ] **Step 5: Commit**

```bash
git add core/network
git commit -m "feat(core-network): add AuthPlugin for bearer token injection and 401 handling"
```

---

### Task 7: core:network — AuthApi.login

**Files:**
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/AuthApi.kt`
- Test: `core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/AuthApiTest.kt`

**Interfaces:**
- Consumes: `ApiResult` (Task 4), `elsfmJson()` (Task 4), `User`/`LoginRequest`/`LoginResponse`/`LaravelValidationError` (Task 3).
- Produces: `class AuthApi(httpClient: HttpClient) { suspend fun login(email: String, password: String, tokenName: String): ApiResult<User> }`. Consumed by `feature:auth` (Task 11).

- [ ] **Step 1: Write the failing test**

`core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/AuthApiTest.kt`:
```kotlin
package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.elsfmJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthApiTest {

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
    }

    @Test
    fun `login success returns user with access token`() = runTest {
        val body = """
            {"status":"success","user":{"id":207,"email":"test.elsfm@gmail.com","access_token":"1|abc"}}
        """.trimIndent()
        val authApi = AuthApi(clientReturning(HttpStatusCode.OK, body))

        val result = authApi.login("test.elsfm@gmail.com", "secret", "android-uuid-1")

        assertTrue(result is ApiResult.Success)
        assertEquals("1|abc", (result as ApiResult.Success).data.accessToken)
    }

    @Test
    fun `login with invalid credentials returns validation error`() = runTest {
        val body = """
            {"message":"The given data was invalid.","errors":{"email":["These credentials do not match our records."]}}
        """.trimIndent()
        val authApi = AuthApi(clientReturning(HttpStatusCode.UnprocessableEntity, body))

        val result = authApi.login("test.elsfm@gmail.com", "wrong", "android-uuid-1")

        assertTrue(result is ApiResult.ValidationError)
        assertEquals(
            listOf("These credentials do not match our records."),
            (result as ApiResult.ValidationError).fields["email"],
        )
    }

    @Test
    fun `login with 401 response returns Unauthorized`() = runTest {
        val authApi = AuthApi(clientReturning(HttpStatusCode.Unauthorized, "{}"))

        val result = authApi.login("test.elsfm@gmail.com", "secret", "android-uuid-1")

        assertTrue(result is ApiResult.Unauthorized)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:network:test --console=plain`
Expected: FAIL — `AuthApi` does not exist yet.

- [ ] **Step 3: Write the implementation**

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/AuthApi.kt`:
```kotlin
package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.LaravelValidationError
import com.elsfm.mobile.core.model.LoginRequest
import com.elsfm.mobile.core.model.LoginResponse
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.io.IOException
import javax.inject.Inject

class AuthApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun login(email: String, password: String, tokenName: String): ApiResult<User> {
        return try {
            val response = httpClient.post("api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email = email, password = password, tokenName = tokenName))
            }
            when (response.status) {
                HttpStatusCode.OK -> ApiResult.Success(response.body<LoginResponse>().user)
                HttpStatusCode.UnprocessableEntity -> {
                    val error = response.body<LaravelValidationError>()
                    ApiResult.ValidationError(error.errors)
                }
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> ApiResult.Unauthorized
                else -> ApiResult.NetworkError(IllegalStateException("Unexpected status ${response.status}"))
            }
        } catch (e: IOException) {
            ApiResult.NetworkError(e)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:network:test --console=plain`
Expected: `BUILD SUCCESSFUL`, 3 new tests passed (9 total in the module).

- [ ] **Step 5: Commit**

```bash
git add core/network
git commit -m "feat(core-network): add AuthApi.login against the real mobile auth endpoint"
```

---

### Task 8: core:network — Hilt NetworkModule wiring

**Files:**
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/ElsfmApiConfig.kt`
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/di/NetworkModule.kt`

**Interfaces:**
- Consumes: `elsfmJson()`, `AuthPlugin` (Task 4, 6), `SessionManager` (Task 5), `EncryptedTokenStore`/`TokenStore` (Task 5).
- Produces: a Hilt-injectable `HttpClient` singleton and a `TokenStore` binding, available to `feature:auth` (Task 11) via constructor injection.

This task has no new business logic to unit test — it is Hilt dependency wiring, verified by successful compilation (later exercised end-to-end by the instrumented test in Task 14).

- [ ] **Step 1: Write the API config and Hilt module**

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/ElsfmApiConfig.kt`:
```kotlin
package com.elsfm.mobile.core.network

object ElsfmApiConfig {
    const val BASE_URL = "https://www.elsfm.com/"
}
```

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/di/NetworkModule.kt`:
```kotlin
package com.elsfm.mobile.core.network.di

import com.elsfm.mobile.core.network.ElsfmApiConfig
import com.elsfm.mobile.core.network.auth.AuthPlugin
import com.elsfm.mobile.core.network.auth.EncryptedTokenStore
import com.elsfm.mobile.core.network.auth.SessionManager
import com.elsfm.mobile.core.network.auth.TokenStore
import com.elsfm.mobile.core.network.elsfmJson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindsModule {
    @Binds
    @Singleton
    abstract fun bindTokenStore(impl: EncryptedTokenStore): TokenStore
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(sessionManager: SessionManager): HttpClient {
        return HttpClient(OkHttp) {
            defaultRequest { url(ElsfmApiConfig.BASE_URL) }
            install(ContentNegotiation) { json(elsfmJson()) }
            install(AuthPlugin) { this.sessionManager = sessionManager }
            install(Logging) { level = LogLevel.INFO }
        }
    }
}
```

- [ ] **Step 2: Verify the module compiles**

Run: `./gradlew :core:network:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add core/network
git commit -m "feat(core-network): wire Hilt NetworkModule with AuthPlugin-backed HttpClient"
```

---

### Task 9: core:database — Room UserDao

**Files:**
- Create: `core/database/build.gradle.kts`
- Create: `core/database/src/main/kotlin/com/elsfm/mobile/core/database/UserEntity.kt`
- Create: `core/database/src/main/kotlin/com/elsfm/mobile/core/database/UserDao.kt`
- Create: `core/database/src/main/kotlin/com/elsfm/mobile/core/database/AppDatabase.kt`
- Create: `core/database/src/main/kotlin/com/elsfm/mobile/core/database/di/DatabaseModule.kt`
- Test: `core/database/src/androidTest/kotlin/com/elsfm/mobile/core/database/UserDaoTest.kt`

**Interfaces:**
- Produces: `@Entity data class UserEntity(id: Int, username: String?, name: String?, email: String, avatarUrl: String?)`, `interface UserDao { suspend fun upsert(user: UserEntity); suspend fun get(): UserEntity?; suspend fun clear() }`, `abstract class AppDatabase : RoomDatabase() { abstract fun userDao(): UserDao }`. Consumed by `feature:auth` (Task 11).

- [ ] **Step 1: Write the failing test**

`core/database/src/androidTest/kotlin/com/elsfm/mobile/core/database/UserDaoTest.kt`:
```kotlin
package com.elsfm.mobile.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertThenGetReturnsTheStoredUser() = runTest {
        val entity = UserEntity(id = 207, username = null, name = "ELSFM APP", email = "test.elsfm@gmail.com", avatarUrl = null)

        userDao.upsert(entity)
        val result = userDao.get()

        assertEquals(entity, result)
    }

    @Test
    fun upsertReplacesThePreviousSingleCachedUser() = runTest {
        userDao.upsert(UserEntity(id = 1, username = null, name = "First", email = "a@b.com", avatarUrl = null))
        userDao.upsert(UserEntity(id = 2, username = null, name = "Second", email = "c@d.com", avatarUrl = null))

        val result = userDao.get()

        assertEquals(2, result?.id)
    }

    @Test
    fun clearRemovesTheCachedUser() = runTest {
        userDao.upsert(UserEntity(id = 1, username = null, name = "First", email = "a@b.com", avatarUrl = null))

        userDao.clear()

        assertNull(userDao.get())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:database:connectedAndroidTest --console=plain`
Expected: FAIL to compile — `core/database` module does not exist yet. (This requires the `Pixel_10_Pro` emulator running — see Task 15's boot command if it isn't already running.)

- [ ] **Step 3: Write the module and implementation**

`core/database/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.elsfm.mobile.core.database"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
```

`core/database/src/main/kotlin/com/elsfm/mobile/core/database/UserEntity.kt`:
```kotlin
package com.elsfm.mobile.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_user")
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String?,
    val name: String?,
    val email: String,
    val avatarUrl: String?,
)
```

`core/database/src/main/kotlin/com/elsfm/mobile/core/database/UserDao.kt`:
```kotlin
package com.elsfm.mobile.core.database

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM cached_user LIMIT 1")
    suspend fun get(): UserEntity?

    @Query("DELETE FROM cached_user")
    suspend fun clear()
}
```

`core/database/src/main/kotlin/com/elsfm/mobile/core/database/AppDatabase.kt`:
```kotlin
package com.elsfm.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
```

`core/database/src/main/kotlin/com/elsfm/mobile/core/database/di/DatabaseModule.kt`:
```kotlin
package com.elsfm.mobile.core.database.di

import android.content.Context
import androidx.room.Room
import com.elsfm.mobile.core.database.AppDatabase
import com.elsfm.mobile.core.database.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "elsfm.db").build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
}
```

Note: `core:database`'s Gradle module doesn't apply the Hilt plugin (`DatabaseModule` only needs `@Module`/`@Provides`, which don't require Hilt's own code generation in this module — Hilt aggregates modules from any module on the app's classpath as long as the module class itself compiles, which only needs the `dagger` annotation processor artifacts already pulled in transitively via `core:network`'s Hilt dependency). If Hilt's KSP step reports it cannot find `@Module` processing in this module during Task 14's full app build, add `alias(libs.plugins.hilt)` + `implementation(libs.hilt.android)` + `ksp(libs.hilt.compiler)` to this module's `build.gradle.kts` and re-run.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:database:connectedAndroidTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 3 tests passed on the connected emulator/device.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts core/database
git commit -m "feat(core-database): add Room UserDao for cached profile storage"
```

---

### Task 10: core:designsystem — Compose theme

**Files:**
- Create: `core/designsystem/build.gradle.kts`
- Create: `core/designsystem/src/main/kotlin/com/elsfm/mobile/core/designsystem/Color.kt`
- Create: `core/designsystem/src/main/kotlin/com/elsfm/mobile/core/designsystem/Type.kt`
- Create: `core/designsystem/src/main/kotlin/com/elsfm/mobile/core/designsystem/Theme.kt`
- Test: `core/designsystem/src/androidTest/kotlin/com/elsfm/mobile/core/designsystem/ElsfmThemeTest.kt`

**Interfaces:**
- Produces: `@Composable fun ElsfmTheme(content: @Composable () -> Unit)`. Consumed by `feature:auth` (Task 12) and `app` (Task 14).

- [ ] **Step 1: Write the failing test**

`core/designsystem/src/androidTest/kotlin/com/elsfm/mobile/core/designsystem/ElsfmThemeTest.kt`:
```kotlin
package com.elsfm.mobile.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class ElsfmThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun themeRendersContentWithoutCrashing() {
        composeTestRule.setContent {
            ElsfmTheme {
                Text("Hello ELSFM")
            }
        }

        composeTestRule.onNodeWithText("Hello ELSFM").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:designsystem:connectedAndroidTest --console=plain`
Expected: FAIL — `core/designsystem` module and `ElsfmTheme` do not exist yet.

- [ ] **Step 3: Write the module and implementation**

`core/designsystem/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.elsfm.mobile.core.designsystem"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

`core/designsystem/src/main/kotlin/com/elsfm/mobile/core/designsystem/Color.kt`:
```kotlin
package com.elsfm.mobile.core.designsystem

import androidx.compose.ui.graphics.Color

val ElsfmAccent = Color(0xFF1DB954)
val ElsfmBackgroundDark = Color(0xFF121212)
val ElsfmSurfaceDark = Color(0xFF1E1E1E)
val ElsfmOnSurfaceDark = Color(0xFFF5F5F5)
```

`core/designsystem/src/main/kotlin/com/elsfm/mobile/core/designsystem/Type.kt`:
```kotlin
package com.elsfm.mobile.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val ElsfmTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp),
)
```

`core/designsystem/src/main/kotlin/com/elsfm/mobile/core/designsystem/Theme.kt`:
```kotlin
package com.elsfm.mobile.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ElsfmDarkColorScheme = darkColorScheme(
    primary = ElsfmAccent,
    background = ElsfmBackgroundDark,
    surface = ElsfmSurfaceDark,
    onSurface = ElsfmOnSurfaceDark,
)

private val ElsfmLightColorScheme = lightColorScheme(
    primary = ElsfmAccent,
)

@Composable
fun ElsfmTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) ElsfmDarkColorScheme else ElsfmLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ElsfmTypography,
        content = content,
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:designsystem:connectedAndroidTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 1 test passed on the connected emulator/device.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts core/designsystem
git commit -m "feat(core-designsystem): add ElsfmTheme with dark/light color schemes"
```

---

### Task 11: feature:auth — AuthRepository

**Files:**
- Create: `feature/auth/build.gradle.kts`
- Create: `feature/auth/src/main/kotlin/com/elsfm/mobile/feature/auth/AuthRepository.kt`
- Test: `feature/auth/src/test/kotlin/com/elsfm/mobile/feature/auth/AuthRepositoryTest.kt`

**Interfaces:**
- Consumes: `AuthApi.login()` (Task 7), `SessionManager` (Task 5), `UserDao`/`UserEntity` (Task 9), `User` (Task 3), `ApiResult` (Task 4).
- Produces: `class AuthRepository(authApi: AuthApi, sessionManager: SessionManager, userDao: UserDao) { suspend fun login(email: String, password: String): ApiResult<User>; suspend fun logout(); suspend fun restoredUser(): User? }`. Consumed by `feature:auth` (Task 12) and `app` (Task 14).

- [ ] **Step 1: Write the failing test**

`feature/auth/src/test/kotlin/com/elsfm/mobile/feature/auth/AuthRepositoryTest.kt`:
```kotlin
package com.elsfm.mobile.feature.auth

import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AuthApi
import com.elsfm.mobile.core.network.auth.SessionManager
import com.elsfm.mobile.core.network.auth.TokenStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeTokenStore : TokenStore {
    private var token: String? = null
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

private class FakeUserDao : UserDao {
    private var stored: UserEntity? = null
    override suspend fun upsert(user: UserEntity) { stored = user }
    override suspend fun get(): UserEntity? = stored
    override suspend fun clear() { stored = null }
}

class AuthRepositoryTest {
    private val user = User(id = 207, email = "test.elsfm@gmail.com", name = "ELSFM APP", accessToken = "1|abc")

    @Test
    fun `login saves token and caches user on success`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val repository = AuthRepository(
            authApi = FakeAuthApi(ApiResult.Success(user)),
            sessionManager = sessionManager,
            userDao = userDao,
        )

        val result = repository.login("test.elsfm@gmail.com", "secret")

        assertEquals(ApiResult.Success(user), result)
        assertEquals("1|abc", sessionManager.currentToken())
        assertEquals(207, userDao.get()?.id)
    }

    @Test
    fun `login does not store anything on validation error`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val validationError = ApiResult.ValidationError(mapOf("email" to listOf("These credentials do not match our records.")))
        val repository = AuthRepository(
            authApi = FakeAuthApi(validationError),
            sessionManager = sessionManager,
            userDao = userDao,
        )

        val result = repository.login("test.elsfm@gmail.com", "wrong")

        assertEquals(validationError, result)
        assertNull(sessionManager.currentToken())
        assertNull(userDao.get())
    }

    @Test
    fun `logout clears session and cached user`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val repository = AuthRepository(FakeAuthApi(ApiResult.Success(user)), sessionManager, userDao)
        repository.login("test.elsfm@gmail.com", "secret")

        repository.logout()

        assertNull(sessionManager.currentToken())
        assertNull(userDao.get())
    }

    @Test
    fun `restoredUser returns cached user only when a token is present`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val repository = AuthRepository(FakeAuthApi(ApiResult.Success(user)), sessionManager, userDao)

        assertNull(repository.restoredUser())

        repository.login("test.elsfm@gmail.com", "secret")
        assertEquals(207, repository.restoredUser()?.id)
    }

    private class FakeAuthApi(private val result: ApiResult<User>) : AuthApiLike {
        override suspend fun login(email: String, password: String, tokenName: String): ApiResult<User> = result
    }
}
```

Note: this test references `AuthApiLike` and constructs `AuthRepository` with a `FakeAuthApi`. To keep `AuthApi` fakeable without a mocking framework, Step 3 extracts a small `AuthApiLike` interface that `AuthApi` implements.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :feature:auth:test --console=plain`
Expected: FAIL — `feature/auth` module, `AuthRepository`, and `AuthApiLike` do not exist yet.

- [ ] **Step 3: Write the module and implementation**

`feature/auth/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.elsfm.mobile.feature.auth"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

First, add the `AuthApiLike` interface and make `AuthApi` (in `core:network`, Task 7) implement it — modify the existing file:

Modify: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/AuthApi.kt` — change `class AuthApi @Inject constructor(` to `class AuthApi @Inject constructor(` implementing a new interface. Create a new file instead so `core:network` stays the single owner of the interface:

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/AuthApiLike.kt`:
```kotlin
package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult

interface AuthApiLike {
    suspend fun login(email: String, password: String, tokenName: String): ApiResult<User>
}
```

Modify `core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/AuthApi.kt` so the class declaration becomes:
```kotlin
class AuthApi @Inject constructor(
    private val httpClient: HttpClient,
) : AuthApiLike {
    override suspend fun login(email: String, password: String, tokenName: String): ApiResult<User> {
```
(keep the rest of the method body exactly as written in Task 7).

`feature/auth/src/main/kotlin/com/elsfm/mobile/feature/auth/AuthRepository.kt`:
```kotlin
package com.elsfm.mobile.feature.auth

import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AuthApiLike
import com.elsfm.mobile.core.network.auth.SessionManager
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApiLike,
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
) {
    suspend fun login(email: String, password: String): ApiResult<User> {
        val tokenName = "android-${UUID.randomUUID()}"
        val result = authApi.login(email, password, tokenName)
        if (result is ApiResult.Success) {
            val token = result.data.accessToken
            if (token != null) {
                sessionManager.saveToken(token)
                userDao.upsert(result.data.toEntity())
            }
        }
        return result
    }

    suspend fun logout() {
        sessionManager.clear()
        userDao.clear()
    }

    suspend fun restoredUser(): User? {
        val token = sessionManager.currentToken() ?: return null
        val cached = userDao.get() ?: return null
        return User(
            id = cached.id,
            username = cached.username,
            name = cached.name,
            email = cached.email,
            avatarUrl = cached.avatarUrl,
            accessToken = token,
        )
    }

    private fun User.toEntity() = UserEntity(
        id = id,
        username = username,
        name = name,
        email = email,
        avatarUrl = avatarUrl,
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :feature:auth:test --console=plain`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts core/network feature/auth
git commit -m "feat(feature-auth): add AuthRepository coordinating login, session and cache"
```

---

### Task 12: feature:auth — LoginViewModel

**Files:**
- Create: `feature/auth/src/main/kotlin/com/elsfm/mobile/feature/auth/LoginUiState.kt`
- Create: `feature/auth/src/main/kotlin/com/elsfm/mobile/feature/auth/LoginViewModel.kt`
- Test: `feature/auth/src/test/kotlin/com/elsfm/mobile/feature/auth/LoginViewModelTest.kt`

**Interfaces:**
- Consumes: `AuthRepository` (Task 11), `DispatcherProvider` (Task 2).
- Produces: `sealed interface LoginUiState { Idle; Loading; FieldErrors(errors); InvalidCredentials; NetworkError; Success(user) }`, `class LoginViewModel(authRepository: AuthRepository, dispatcherProvider: DispatcherProvider) : ViewModel() { val state: StateFlow<LoginUiState>; fun onLoginClicked(email: String, password: String) }`. Consumed by Task 13's `LoginScreen`.

- [ ] **Step 1: Write the failing test**

`feature/auth/src/test/kotlin/com/elsfm/mobile/feature/auth/LoginViewModelTest.kt`:
```kotlin
package com.elsfm.mobile.feature.auth

import app.cash.turbine.test
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AuthApiLike
import com.elsfm.mobile.core.network.auth.SessionManager
import com.elsfm.mobile.core.network.auth.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeTokenStore : TokenStore {
    private var token: String? = null
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

private class FakeUserDao : UserDao {
    private var stored: UserEntity? = null
    override suspend fun upsert(user: UserEntity) { stored = user }
    override suspend fun get(): UserEntity? = stored
    override suspend fun clear() { stored = null }
}

private class FakeAuthApi(private val result: ApiResult<User>) : AuthApiLike {
    override suspend fun login(email: String, password: String, tokenName: String) = result
}

private class TestDispatcherProvider(dispatcher: kotlinx.coroutines.CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val main = dispatcher
    override val default = dispatcher
}

class LoginViewModelTest {

    @Test
    fun `emits Loading then Success on successful login`() = runTest {
        val user = User(id = 207, email = "test.elsfm@gmail.com", accessToken = "1|abc")
        val repository = AuthRepository(FakeAuthApi(ApiResult.Success(user)), SessionManager(FakeTokenStore()), FakeUserDao())
        val viewModel = LoginViewModel(repository, TestDispatcherProvider(StandardTestDispatcher(testScheduler)))

        viewModel.state.test {
            assertEquals(LoginUiState.Idle, awaitItem())
            viewModel.onLoginClicked("test.elsfm@gmail.com", "secret")
            assertEquals(LoginUiState.Loading, awaitItem())
            assertEquals(LoginUiState.Success(user), awaitItem())
        }
    }

    @Test
    fun `emits FieldErrors on validation failure`() = runTest {
        val errors = mapOf("email" to listOf("These credentials do not match our records."))
        val repository = AuthRepository(FakeAuthApi(ApiResult.ValidationError(errors)), SessionManager(FakeTokenStore()), FakeUserDao())
        val viewModel = LoginViewModel(repository, TestDispatcherProvider(StandardTestDispatcher(testScheduler)))

        viewModel.state.test {
            assertEquals(LoginUiState.Idle, awaitItem())
            viewModel.onLoginClicked("test.elsfm@gmail.com", "wrong")
            assertEquals(LoginUiState.Loading, awaitItem())
            val errorState = awaitItem()
            assertTrue(errorState is LoginUiState.FieldErrors)
            assertEquals(errors, (errorState as LoginUiState.FieldErrors).errors)
        }
    }

    @Test
    fun `emits NetworkError when the repository reports a network failure`() = runTest {
        val repository = AuthRepository(FakeAuthApi(ApiResult.NetworkError(RuntimeException("offline"))), SessionManager(FakeTokenStore()), FakeUserDao())
        val viewModel = LoginViewModel(repository, TestDispatcherProvider(StandardTestDispatcher(testScheduler)))

        viewModel.state.test {
            assertEquals(LoginUiState.Idle, awaitItem())
            viewModel.onLoginClicked("test.elsfm@gmail.com", "secret")
            assertEquals(LoginUiState.Loading, awaitItem())
            assertEquals(LoginUiState.NetworkError, awaitItem())
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :feature:auth:test --console=plain`
Expected: FAIL — `LoginUiState`/`LoginViewModel` do not exist yet.

- [ ] **Step 3: Write the implementation**

`feature/auth/src/main/kotlin/com/elsfm/mobile/feature/auth/LoginUiState.kt`:
```kotlin
package com.elsfm.mobile.feature.auth

import com.elsfm.mobile.core.model.User

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class FieldErrors(val errors: Map<String, List<String>>) : LoginUiState
    data object InvalidCredentials : LoginUiState
    data object NetworkError : LoginUiState
    data class Success(val user: User) : LoginUiState
}
```

`feature/auth/src/main/kotlin/com/elsfm/mobile/feature/auth/LoginViewModel.kt`:
```kotlin
package com.elsfm.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onLoginClicked(email: String, password: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = LoginUiState.Loading
            _state.value = when (val result = authRepository.login(email, password)) {
                is ApiResult.Success -> LoginUiState.Success(result.data)
                is ApiResult.ValidationError -> LoginUiState.FieldErrors(result.fields)
                is ApiResult.Unauthorized -> LoginUiState.InvalidCredentials
                is ApiResult.NetworkError -> LoginUiState.NetworkError
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :feature:auth:test --console=plain`
Expected: `BUILD SUCCESSFUL`, 3 new tests passed (7 total in the module).

- [ ] **Step 5: Commit**

```bash
git add feature/auth
git commit -m "feat(feature-auth): add LoginViewModel with sealed UI state"
```

---

### Task 13: feature:auth — LoginScreen + saved-password support

**Files:**
- Create: `feature/auth/src/main/kotlin/com/elsfm/mobile/feature/auth/PasswordSaver.kt`
- Create: `feature/auth/src/main/kotlin/com/elsfm/mobile/feature/auth/LoginScreen.kt`
- Test: `feature/auth/src/androidTest/kotlin/com/elsfm/mobile/feature/auth/LoginScreenTest.kt`

**Interfaces:**
- Consumes: `LoginViewModel`/`LoginUiState` (Task 12), `ElsfmTheme` (Task 10).
- Produces: `@Composable fun LoginScreen(onLoggedIn: (User) -> Unit, viewModel: LoginViewModel = hiltViewModel())`, `class PasswordSaver(context: Context) { suspend fun save(email: String, password: String) }`. Consumed by `app` (Task 14).

- [ ] **Step 1: Write the failing test**

`feature/auth/src/androidTest/kotlin/com/elsfm/mobile/feature/auth/LoginScreenTest.kt`:
```kotlin
package com.elsfm.mobile.feature.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import com.elsfm.mobile.core.designsystem.ElsfmTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun enteringCredentialsAndTappingLoginInvokesTheCallback() {
        val state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
        var loginClicked: Pair<String, String>? = null

        composeTestRule.setContent {
            ElsfmTheme {
                LoginScreenContent(
                    state = state,
                    onLoginClicked = { email, password -> loginClicked = email to password },
                )
            }
        }

        composeTestRule.onNodeWithTag("email_field").performTextInput("test.elsfm@gmail.com")
        composeTestRule.onNodeWithTag("password_field").performTextInput("secret")
        composeTestRule.onNodeWithTag("login_button").performClick()

        assert(loginClicked == ("test.elsfm@gmail.com" to "secret"))
    }

    @Test
    fun fieldErrorsAreDisplayed() {
        val state = MutableStateFlow<LoginUiState>(
            LoginUiState.FieldErrors(mapOf("email" to listOf("These credentials do not match our records."))),
        )

        composeTestRule.setContent {
            ElsfmTheme {
                LoginScreenContent(state = state, onLoginClicked = { _, _ -> })
            }
        }

        composeTestRule.onNodeWithText("These credentials do not match our records.").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :feature:auth:connectedAndroidTest --console=plain`
Expected: FAIL — `LoginScreenContent` does not exist yet.

- [ ] **Step 3: Write the implementation**

`feature/auth/src/main/kotlin/com/elsfm/mobile/feature/auth/PasswordSaver.kt`:
```kotlin
package com.elsfm.mobile.feature.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException
import javax.inject.Inject

class PasswordSaver @Inject constructor(
    private val context: Context,
) {
    suspend fun save(email: String, password: String) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.createCredential(context, CreatePasswordRequest(id = email, password = password))
        } catch (e: CreateCredentialException) {
            Log.w("PasswordSaver", "Credential Manager declined to save the password", e)
        }
    }
}
```

`feature/auth/src/main/kotlin/com/elsfm/mobile/feature/auth/LoginScreen.kt`:
```kotlin
package com.elsfm.mobile.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoggedIn: (User) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val passwordSaver = remember { PasswordSaver(context) }
    val scope = rememberCoroutineScope()
    var lastCredentials by remember { mutableStateOf<Pair<String, String>?>(null) }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        val currentState = state
        if (currentState is LoginUiState.Success) {
            lastCredentials?.let { (email, password) -> passwordSaver.save(email, password) }
            onLoggedIn(currentState.user)
        }
    }

    LoginScreenContent(
        state = viewModel.state,
        onLoginClicked = { email, password ->
            lastCredentials = email to password
            viewModel.onLoginClicked(email, password)
        },
    )
}

@Composable
fun LoginScreenContent(
    state: StateFlow<LoginUiState>,
    onLoginClicked: (email: String, password: String) -> Unit,
) {
    val currentState by state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val fieldErrors = (currentState as? LoginUiState.FieldErrors)?.errors ?: emptyMap()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            isError = fieldErrors.containsKey("email"),
            modifier = Modifier.fillMaxWidth().testTag("email_field"),
        )
        fieldErrors["email"]?.forEach { message -> Text(message) }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().testTag("password_field"),
        )

        if (currentState == LoginUiState.InvalidCredentials) {
            Text("Incorrect email or password.")
        }
        if (currentState == LoginUiState.NetworkError) {
            Text("Couldn't reach elsfm.com. Check your connection and try again.")
        }

        Button(
            onClick = { onLoginClicked(email, password) },
            enabled = currentState != LoginUiState.Loading,
            modifier = Modifier.testTag("login_button"),
        ) {
            if (currentState == LoginUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.testTag("login_progress"))
            } else {
                Text("Log in")
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :feature:auth:connectedAndroidTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 2 tests passed on the connected emulator/device.

- [ ] **Step 5: Commit**

```bash
git add feature/auth
git commit -m "feat(feature-auth): add LoginScreen with Credential Manager saved-password support"
```

---

### Task 14: app — Hilt application shell, nav host, session-aware start destination

**Files:**
- Modify: `app/src/main/kotlin/com/elsfm/mobile/MainActivity.kt`
- Create: `app/src/main/kotlin/com/elsfm/mobile/ElsfmNavHost.kt`
- Create: `app/src/main/kotlin/com/elsfm/mobile/HomePlaceholderScreen.kt`
- Test: `app/src/androidTest/kotlin/com/elsfm/mobile/LoginFlowInstrumentedTest.kt`

**Interfaces:**
- Consumes: `AuthRepository.restoredUser()` (Task 11), `SessionManager.events` / `SessionEvent.Expired` (Task 5), `LoginScreen` (Task 13), `ElsfmTheme` (Task 10).
- Produces: the app's navigation graph — `"login"` and `"home"` routes.

- [ ] **Step 1: Write the failing instrumented test**

`app/src/androidTest/kotlin/com/elsfm/mobile/LoginFlowInstrumentedTest.kt`:
```kotlin
package com.elsfm.mobile

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LoginFlowInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun appLaunchesToLoginScreenWhenNoSessionIsStored() {
        composeTestRule.onNodeWithTag("email_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("password_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_button").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedAndroidTest --console=plain`
Expected: FAIL — `MainActivity` still shows the placeholder `Text("ELSFM")` from Task 1, not the login screen.

- [ ] **Step 3: Write the nav host, home placeholder, and wire MainActivity**

`app/src/main/kotlin/com/elsfm/mobile/HomePlaceholderScreen.kt`:
```kotlin
package com.elsfm.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.elsfm.mobile.core.model.User

@Composable
fun HomePlaceholderScreen(user: User, onLogoutClicked: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Logged in as ${user.email}")
        Button(onClick = onLogoutClicked) {
            Text("Log out")
        }
    }
}
```

`app/src/main/kotlin/com/elsfm/mobile/ElsfmNavHost.kt`:
```kotlin
package com.elsfm.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.elsfm.mobile.core.network.auth.SessionEvent
import com.elsfm.mobile.feature.auth.LoginScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_HOME = "home"

@Composable
fun ElsfmNavHost(
    navController: NavHostController = rememberNavController(),
    startDestinationViewModel: StartDestinationViewModel = hiltViewModel(),
) {
    val startState by startDestinationViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        startDestinationViewModel.sessionEvents.collect { event ->
            if (event is SessionEvent.Expired) {
                navController.navigate(ROUTE_LOGIN) {
                    popUpTo(0)
                }
            }
        }
    }

    when (val current = startState) {
        StartDestinationState.Loading -> Unit
        is StartDestinationState.Resolved -> {
            NavHost(navController = navController, startDestination = current.route) {
                composable(ROUTE_LOGIN) {
                    LoginScreen(onLoggedIn = {
                        navController.navigate(ROUTE_HOME) { popUpTo(0) }
                    })
                }
                composable(ROUTE_HOME) {
                    val user = current.restoredUser
                    if (user != null) {
                        HomePlaceholderScreen(
                            user = user,
                            onLogoutClicked = {
                                startDestinationViewModel.logout()
                                navController.navigate(ROUTE_LOGIN) { popUpTo(0) }
                            },
                        )
                    }
                }
            }
        }
    }
}
```

This references a `StartDestinationViewModel`/`StartDestinationState` that don't exist yet. Both use the same `"login"`/`"home"` route string values as this file's `ROUTE_LOGIN`/`ROUTE_HOME` constants — those constants are file-private, so `StartDestinationViewModel.kt` below uses the literal strings directly rather than importing them. Create:

`app/src/main/kotlin/com/elsfm/mobile/StartDestinationViewModel.kt`:
```kotlin
package com.elsfm.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.auth.SessionEvent
import com.elsfm.mobile.core.network.auth.SessionManager
import com.elsfm.mobile.feature.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StartDestinationState {
    data object Loading : StartDestinationState
    data class Resolved(val route: String, val restoredUser: User?) : StartDestinationState
}

@HiltViewModel
class StartDestinationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<StartDestinationState>(StartDestinationState.Loading)
    val state: StateFlow<StartDestinationState> = _state.asStateFlow()

    val sessionEvents: SharedFlow<SessionEvent> = sessionManager.events

    init {
        viewModelScope.launch {
            val restoredUser = authRepository.restoredUser()
            _state.value = StartDestinationState.Resolved(
                route = if (restoredUser != null) "home" else "login",
                restoredUser = restoredUser,
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
```

Modify `app/src/main/kotlin/com/elsfm/mobile/MainActivity.kt` to:
```kotlin
package com.elsfm.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.elsfm.mobile.core.designsystem.ElsfmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ElsfmTheme {
                ElsfmNavHost()
            }
        }
    }
}
```

Update `app/build.gradle.kts` dependencies to add the Hilt testing block for instrumented tests (append inside the existing `dependencies {}` block from Task 1 — these three lines were already present in Task 1's file; verify they're there):
```kotlin
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
```

Create the required Hilt test application runner — `app/src/androidTest/kotlin/com/elsfm/mobile/HiltTestRunner.kt`:
```kotlin
package com.elsfm.mobile

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
```

Modify `app/build.gradle.kts`'s `defaultConfig` block to point at it:
```kotlin
        testInstrumentationRunner = "com.elsfm.mobile.HiltTestRunner"
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:connectedAndroidTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 1 test passed — the app launches directly to the login screen because no session token is stored on a fresh emulator install.

- [ ] **Step 5: Commit**

```bash
git add app
git commit -m "feat(app): wire Hilt nav host with session-aware start destination"
```

---

### Task 15: Build, install on the emulator, manual verification, and push to GitHub

**Files:** none (verification + repo operations only).

- [ ] **Step 1: Run the full test suite**

```bash
cd /Users/siku/Documents/GitHub/elsfm-native
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew test --console=plain
```
Expected: `BUILD SUCCESSFUL`, all unit tests across every module pass.

- [ ] **Step 2: Boot the existing emulator**

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
nohup "$ANDROID_HOME/emulator/emulator" -avd Pixel_10_Pro -no-snapshot-load > /tmp/emulator.log 2>&1 &
"$ANDROID_HOME/platform-tools/adb" wait-for-device
until [ "$("$ANDROID_HOME/platform-tools/adb" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
```
Expected: the command returns once `Pixel_10_Pro` has finished booting.

- [ ] **Step 3: Run the full connected instrumented test suite**

```bash
./gradlew connectedAndroidTest --console=plain
```
Expected: `BUILD SUCCESSFUL`, all instrumented tests (Room, Compose UI, the Hilt login-flow test) pass on the booted emulator.

- [ ] **Step 4: Install and manually confirm the app launches**

```bash
./gradlew :app:installDebug --console=plain
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.elsfm.mobile/.MainActivity
"$ANDROID_HOME/platform-tools/adb" logcat -d | grep -i "elsfm\|AndroidRuntime" | tail -50
```
Expected: no crash in logcat; the emulator screen shows the Login screen. At this point, **you** (not the assistant) should type the test account's real credentials into the running app on the emulator once, to confirm the live `/api/v1/auth/login` call succeeds end-to-end and the app transitions to the "Logged in as ..." placeholder screen — this is the one manual real-credential check called out in the Global Constraints section.

- [ ] **Step 5: Create the GitHub repository and push**

```bash
cd /Users/siku/Documents/GitHub/elsfm-native
gh repo create dewanlabung/elsfm-native --private --source=. --remote=origin
git branch -M main
git push -u origin main
```
Expected: the repo is created under `github.com/dewanlabung/elsfm-native` and all local commits from Tasks 1–14 (plus the Phase 1 design spec) are pushed to `main`.

- [ ] **Step 6: Verify the pushed state**

```bash
gh repo view dewanlabung/elsfm-native --json name,visibility,defaultBranchRef
git log --oneline
```
Expected: `visibility: PRIVATE`, and the commit log shows the full Phase 1 history in order.

---

## What's deliberately not in this plan

Playback (Media3/ExoPlayer), play-history recording, library/search/playlists/artist/album browsing, downloads, recommendations, lyrics, notifications, and profile editing are out of scope for Phase 1 per the design spec — each becomes its own spec + plan (Phase 2: Player; Phase 3: Library/Playlists; Phase 4: the rest) once this Foundation phase is reviewed and merged.
