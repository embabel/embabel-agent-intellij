import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.embabel.agent"
version = "1.0.0"

// IntelliJ IDEA 2025.3 ships with JBR 21. Kotlin 2.x is required for
// targeting 2025.1 or later per the IntelliJ Platform SDK docs.
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// UI tests need access to the production plugin classes because they install the
// built plugin and also reference shared constants such as inspection messages.
val integrationTestSourceSet = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}

// This source set builds a second, test-only IntelliJ plugin. It depends on the
// production sources so the probe can invoke the real inspection implementation.
val integrationTestPluginSourceSet = sourceSets.create("integrationTestPlugin") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}

// Reuse the normal test dependency buckets for integrationTest so JUnit and the
// rest of the test stack stay defined in one place.
configurations[integrationTestSourceSet.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

// The auxiliary integration-test plugin behaves like production plugin code, not
// like a unit test. It therefore inherits the main implementation/runtime
// dependencies instead of testImplementation/testRuntimeOnly.
configurations[integrationTestPluginSourceSet.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[integrationTestPluginSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 2025.3 unified IntelliJ IDEA (Community + Ultimate merged)
        intellijIdea("2025.3.4")
        bundledPlugin("com.intellij.java")
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
        testFramework(TestFrameworkType.Starter, configurationName = integrationTestSourceSet.implementationConfigurationName,
        )
    }
    testImplementation("junit:junit:4.13.2")
    add(integrationTestSourceSet.implementationConfigurationName, "org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
    add(integrationTestSourceSet.implementationConfigurationName, "org.junit.jupiter:junit-jupiter:5.13.4")
    add(integrationTestSourceSet.runtimeOnlyConfigurationName, "org.junit.jupiter:junit-jupiter-engine:5.13.4")
    add(integrationTestSourceSet.runtimeOnlyConfigurationName, "org.junit.platform:junit-platform-launcher:1.13.4")
    add(integrationTestSourceSet.runtimeOnlyConfigurationName, "com.intellij.platform:kotlinx-coroutines-core-jvm:1.10.1-intellij-5")
    add(integrationTestSourceSet.runtimeOnlyConfigurationName, "org.jetbrains.kotlin:kotlin-reflect:2.2.20")
    add(integrationTestSourceSet.runtimeOnlyConfigurationName, "org.kodein.di:kodein-di-jvm:7.26.1")
    add(integrationTestPluginSourceSet.implementationConfigurationName, "org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
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
            ide("IC-2025.2")
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

val integrationTest by intellijPlatformTesting.testIdeUi.registering {
    task {
        description = "Runs integration tests against a real IntelliJ IDEA instance."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        testClassesDirs = integrationTestSourceSet.output.classesDirs
        classpath = integrationTestSourceSet.runtimeClasspath
        useJUnitPlatform()
        shouldRunAfter(tasks.test)
        dependsOn(tasks.buildPlugin, integrationTestProbePluginZip)
        // The UI test installs two plugins:
        // 1. the production plugin zip under test
        // 2. a dedicated test-only plugin that exposes a Driver-callable project service
        systemProperty(
            "path.to.build.plugin",
            layout.buildDirectory.file("distributions/${project.name}-${project.version}.zip").get().asFile.absolutePath,
        )
        systemProperty(
            "path.to.integration.test.plugin",
            integrationTestProbePluginZip.flatMap { it.archiveFile }.get().asFile.absolutePath,
        )
    }
}

val integrationTestProbePluginJar by tasks.registering(Jar::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Builds the test-only IntelliJ plugin used by integrationTest."
    archiveBaseName.set("embabel-agent-intellij-integration-test-plugin")
    dependsOn(tasks.named(integrationTestPluginSourceSet.classesTaskName), tasks.named(integrationTestPluginSourceSet.processResourcesTaskName))
    // Use the raw descriptor from src/integrationTestPlugin/resources rather than the
    // patched production descriptor that the IntelliJ Gradle plugin generates.
    from(integrationTestPluginSourceSet.output.classesDirs)
    from("src/integrationTestPlugin/resources")
}

val integrationTestProbePluginZip by tasks.registering(Zip::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Packages the test-only IntelliJ plugin used by integrationTest."
    archiveBaseName.set("embabel-agent-intellij-integration-test")
    dependsOn(integrationTestProbePluginJar)
    // IntelliJ installs plugins from a directory-style zip layout:
    // <plugin-id>/lib/<plugin-jar>.
    from(integrationTestProbePluginJar) {into("embabel-agent-intellij-integration-test/lib")}
}
