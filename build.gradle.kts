import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij.platform") version "2.2.1"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "2.2.0"
    // detekt linter - read more: https://detekt.github.io/detekt/gradle.html
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

// Import variables from gradle.properties file
val pluginGroup: String by project

// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
@Suppress("PropertyName")
val pluginName_: String by project

val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project
val pluginVerifierIdeVersions: String by project

val platformType: String by project
val platformVersion: String by project
val platformPlugins: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = pluginVersion

// Configure Java toolchain - IntelliJ 2024.2+ requires JVM 21
kotlin {
    jvmToolchain(21)
}

// Configure project's dependencies
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")
    implementation("com.trendyol:kediatr-core:1.0.15")

    intellijPlatform {
        intellijIdeaCommunity(platformVersion)

        // Plugin Dependencies
        bundledPlugins(platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty))

        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = pluginName_
        version = pluginVersion

        ideaVersion {
            sinceBuild = pluginSinceBuild
            untilBuild = pluginUntilBuild
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// Configure detekt plugin.
// Read more: https://detekt.github.io/detekt/kotlindsl.html
detekt {
    config.setFrom(files("./detekt-config.yml"))
    buildUponDefaultConfig = true
}

tasks {
    // Set the compatibility versions to 21 for IntelliJ 2024.2+
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    withType<Detekt> {
        jvmTarget = "21"
    }

    // Disable buildSearchableOptions task
    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            File("./README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) },
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(
            changelog.renderItem(
                changelog.getLatest(),
                org.jetbrains.changelog.Changelog.OutputType.HTML,
            ),
        )
    }

    runIde {
        // Enable K2 mode for testing
        jvmArgs("-Didea.kotlin.plugin.use.k2=true")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(pluginVersion.split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(pluginVersion)
    path.set("${project.projectDir}/CHANGELOG.md")
    header.set(provider { "[${version.get()}]" })
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}
