plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.embabel.agent"
version = "1.0.0"

// IntelliJ IDEA 2025.3 ships with JBR 21. Kotlin 2.x is required for
// targeting 2025.1 or later per the IntelliJ Platform SDK docs.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 2025.3 unified IntelliJ IDEA (Community + Ultimate merged) — use intellijIdea()
        intellijIdea("2025.3.4")
        bundledPlugin("com.intellij.java")
        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.embabel.agent.intellij-plugin"
        name = "Embabel Agent"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "233"
            untilBuild = provider { null } // no upper cap — plugin works on all future IDEs
        }
    }
    pluginVerification {
        ides {
            // Capped at 253.* to avoid a Gradle tar-extraction bug with the 2026.1 IDE
            // (gradle-api-9.3.0.jar fails to copy on Linux CI runners).
            // Remove the untilBuild cap here once the upstream bug is resolved.
            select {
                types = listOf(
                    org.jetbrains.intellij.platform.gradle.models.IntelliJPlatformType.IntellijIdeaUltimate
                )
                channels = listOf(
                    org.jetbrains.intellij.platform.gradle.models.ProductRelease.Channel.RELEASE
                )
                sinceBuild = "233"
                untilBuild = "253.*"
            }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}
