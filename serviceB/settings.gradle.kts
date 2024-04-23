pluginManagement {
    includeBuild("../build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "serviceB"
include(
    "domain",
    "presentation:service",
    "presentation:acceptanceTest",
    "infrastructure:adapter1"
)
