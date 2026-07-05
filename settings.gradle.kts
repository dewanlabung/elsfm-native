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
include(":core:media")
include(":feature:auth")
include(":feature:player")
include(":feature:artist")
include(":feature:search")
include(":feature:library")
include(":feature:profile")
include(":feature:discovery")
include(":feature:downloads")
