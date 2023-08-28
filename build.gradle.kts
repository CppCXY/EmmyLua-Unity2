
import de.undercouch.gradle.tasks.download.*

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.intellij") version "1.10.0"
    id("de.undercouch.download").version("5.3.0")
}

group = "com.cppcxy"
version = "1.3.1"

val emmyluaUnityLsVersion = "1.3.1"
val emmyluaUnityLsProjectUrl = "https://github.com/CppCXY/EmmyLua-Unity-LS"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set("EmmyLua-Unity")
    version.set("2022.3")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("com.tang:1.3.8-IDEA223"))
}

task("downloadLs", type = Download::class) {
    src(arrayOf(
            "${emmyluaUnityLsProjectUrl}/releases/download/${emmyluaUnityLsVersion}/darwin-arm64.zip",
            "${emmyluaUnityLsProjectUrl}/releases/download/${emmyluaUnityLsVersion}/darwin-x64.zip",
            "${emmyluaUnityLsProjectUrl}/releases/download/${emmyluaUnityLsVersion}/linux-x64.zip",
            "${emmyluaUnityLsProjectUrl}/releases/download/${emmyluaUnityLsVersion}/win32-x64.zip",
    ))

    dest("temp")
}

task("unzipLs", type = Copy::class) {
    dependsOn("downloadLs")
    from(zipTree("temp/win32-x64.zip")) {
        into("bin")
    }
    from(zipTree("temp/darwin-x64.zip")) {
        into("bin")
    }
    from(zipTree("temp/darwin-arm64.zip")) {
        into("bin")
    }
    from(zipTree("temp/linux-x64.zip")) {
        into("bin")
    }
    destinationDir = file("temp")
}

task("installLs", type = Copy::class) {
    dependsOn("unzipLs")
    from("temp/bin/win32-x64/") {
        include("unity.exe")
        into("bin/win32-x64/")
    }
    from("temp/bin/linux-x64/") {
        include("unity")
        into("bin/linux-x64/")
    }
    from("temp/bin/darwin-x64/") {
        include("unity")
        into("bin/darwin-x64/")
    }
    from("temp/bin/darwin-arm64/") {
        include("unity")
        into("bin/darwin-arm64/")
    }
    destinationDir = file("src/main/resources/unity")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("232.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    buildPlugin {
        dependsOn("installLs")
    }

    withType<org.jetbrains.intellij.tasks.PrepareSandboxTask> {
        doLast {
            copy {
                from("src/main/resources/unity")
                into("$destinationDir/${pluginName.get()}/unity")
            }
        }
    }
}
