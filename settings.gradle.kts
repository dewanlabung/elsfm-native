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
