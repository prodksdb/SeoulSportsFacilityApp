pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

    // Version Catalog 설정 추가
    //versionCatalogs {
    //   create("libs") {
    //  from(files("gradle/libs.versions.toml"))
    // }
    //}
}

rootProject.name = "SeoulData"
include(":app")
 