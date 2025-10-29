pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}


dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}


rootProject.name = "Compose-vs-Android-View-System-Performance"
include(":shared", ":app-compose", ":app-view")