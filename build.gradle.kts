import org.gradle.kotlin.dsl.publishMods
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.10"
    id("me.modmuss50.mod-publish-plugin") version "0.8.1"
}

val maven_group: String by project
val archives_base_name: String by project
val license: String by project
val mod_version: String by project
val kotlinmcui_version: String by project
val mod_id: String by project
val mod_name: String by project
val mod_description: String by project
val mod_authors: String by project
val mod_github: String by project
val mod_issues: String by project
val mod_mcmod: String by project
val mod_modrinth: String by project
val mod_cuseforge: String by project
val icon: String by project

group = maven_group
version = mod_version

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.2894638479:KotlinMCUI:v$kotlinmcui_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xcontext-parameters"))
}

val sourcesJar: org.gradle.jvm.tasks.Jar by tasks
sourcesJar.exclude("fabric.mod.json")
sourcesJar.exclude("META-INF/mods.toml")
sourcesJar.exclude("META-INF/neoforge.mods.toml")
sourcesJar.from("LICENSE")

tasks.jar {
    from("LICENSE")
}

tasks.processResources {
    val map = mapOf(
        "license" to license,
        "mod_version" to mod_version,
        "kotlinmcui_version" to kotlinmcui_version,
        "mod_id" to mod_id,
        "mod_name" to mod_name,
        "mod_description" to mod_description,
        "mod_authors" to mod_authors,
        "mod_github" to mod_github,
        "mod_issues" to mod_issues,
        "mod_mcmod" to mod_mcmod,
        "mod_modrinth" to mod_modrinth,
        "mod_cuseforge" to mod_cuseforge,
        "icon" to icon,
    )
    inputs.properties(map)
    filesMatching("fabric.mod.json") { expand(map) }
    filesMatching("META-INF/mods.toml") { expand(map) }
    filesMatching("META-INF/neoforge.mods.toml") { expand(map) }
    filesMatching("pack.mcmeta") { expand(map) }
}

val tag = "v$version"

val shouldPublish by lazy {
    providers.exec {
        commandLine("git", "ls-remote", "--tags", "origin", "refs/tags/$tag")
    }.standardOutput.asText.get().isBlank()
}
tasks.configureEach {
    if(group == "publishing") enabled = shouldPublish
}

val jar: org.gradle.jvm.tasks.Jar by tasks
publishMods {
    file = jar.archiveFile
    additionalFiles = files(sourcesJar.archiveFile)
    changelog = "no changelog."
    type = when {
        mod_version.contains("SNAPSHOT",true) -> ALPHA
        mod_version.contains("alpha",true) -> ALPHA
        mod_version.contains("beta",true) -> BETA
        else -> STABLE
    }
    displayName = "Pumpkin Sim $version"
    modLoaders.addAll("forge","fabric","neoforge")

    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = "BcXzFtbh"
        requires("kotlinmcui")
        minecraftVersionRange {
            start = "1.14"
            end = "latest"
        }
    }
    curseforge {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        projectId = "1514126"
        requires("kotlinmcui")
        clientRequired = true
        serverRequired = false
        minecraftVersionRange {
            start = "1.14"
            end = "latest"
        }
    }
    github {
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        repository = "2894638479/PumpkinSim"
        commitish = "master"
        tagName = tag
    }
}
