plugins {
    id("kotlin-application-conventions")
}

dependencies {
    implementation("org.apache.commons:commons-text")
    //implementation(project(":utilities"))
}

application {
    // Define the main class for the application.
    mainClass.set("eu.danielz.mono.app.AppKt")
}
