plugins {
    kotlin("jvm") version "1.8.20"
    kotlin("kapt") version "1.8.20"
//    kotlin("android")
    id("distribution")
//    id("com.android.application") version "8.0.0" apply false
    id("com.vanniktech.maven.publish") version "0.17.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
//    id("org.jetbrains.compose")
//    id("com.android.application")
}

allprojects {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven(url ="https://jitpack.io")
    }
}

ktlint {
    debug.set(false)
    verbose.set(true)
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

val sep = File.separator
distributions {
    main {
        contents {
            from("build${sep}libs")
            from("build${sep}publications${sep}maven")
        }
    }
}

val publish = extensions.getByType(
    com.vanniktech.maven.publish.MavenPublishPluginExtension::class.java
)
// signing is done when uploading files to MC
// via gpg:sign-and-deploy-file (release.kts)
publish.releaseSigningEnabled = false

tasks.named("distZip") {
    dependsOn("publishToMavenLocal")
    onlyIf {
        inputs.sourceFiles.isEmpty.not().also {
            require(it) { "No distribution to zip." }
        }
    }
}

repositories {
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//    maven(url ="https://jitpack.io")
}

dependencies {
//    implementation("androidx.compose.ui:ui-android")
//    testImplementation("androidx.compose.ui:ui-android:1.5.0")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    kapt("com.google.auto.service:auto-service:1.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")

    testImplementation(platform("androidx.compose:compose-bom:2022.10.00"))
//    implementation("androidx.compose.runtime:runtime:1.5.0")
//    implementation("androidx.compose.foundation:foundation:1.5.0")
//    implementation("androidx.compose.ui:ui-tooling-android:1.5.0")

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
    testImplementation("org.jetbrains.compose.desktop:desktop:1.5.0-beta02")

//    testImplementation(platform("androidx.compose:compose-bom:2023.06.01"))
//    implementation("androidx.compose.ui:ui:1.4.3")
//    testImplementation("androidx.compose.ui:ui:1.4.3")
////    testImplementation("androidx.compose.ui:ui-graphics")
//    testImplementation("androidx.compose.ui:ui-tooling-preview")
//    testImplementation("androidx.compose.material3:material3")

//    testImplementation(platform("androidx.compose:compose-bom:2023.06.01"))
//    testImplementation("androidx.compose.runtime:runtime:1.5.0")
//    testImplementation("androidx.compose.foundation:foundation:1.5.0")
//    testImplementation("androidx.compose.ui:ui-tooling-android:1.5.0")
//    testImplementation("org.jetbrains.compose.ui:ui:1.4.0")
}

plugins.withId("com.vanniktech.maven.publish.base") {
    configure<PublishingExtension> {
        repositories {
            maven {
                name = "mavenTestRepo"
                url = file("${rootProject.projectDir}/../build/mavenTestRepo").toURI()
            }
        }
    }
}
