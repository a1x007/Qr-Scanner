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
        jcenter()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://chaquo.com/maven") }
        flatDir {
            dirs("libs")
        }
    }
}


rootProject.name = "Qr Scanner"
include(":app", ":custom_qr_generator")

