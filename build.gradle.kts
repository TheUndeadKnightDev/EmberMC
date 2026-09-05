import io.papermc.paperweight.checkstyle.PaperCheckstyleExt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    id("io.papermc.paperweight.patcher") version "2.0.0-beta.21"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

/*
 * EmberMC is a paperweight-patcher fork of Paper.
 *
 * Nothing under paper-api/, paper-server/, paper-checkstyle/ or .checkstyle/ is
 * committed here: paperweight checks out the Paper commit named in
 * gradle.properties, generates those directories, and applies our patches on
 * top. Our own code lives in ember-*\/src, and our changes to Paper live as
 * patches under ember-*\/paper-patches (Paper's own classes) and
 * ember-server/minecraft-patches (Mojang classes). See UPDATING-UPSTREAM.md.
 */
paperweight {
    upstreams.paper {
        ref = providers.gradleProperty("paperCommit")

        // The three upstream build scripts are patched rather than replaced, so
        // upstream dependency bumps flow through automatically.
        patchFile {
            path = "paper-server/build.gradle.kts"
            outputFile = file("ember-server/build.gradle.kts")
            patchFile = file("ember-server/build.gradle.kts.patch")
        }
        patchFile {
            path = "paper-api/build.gradle.kts"
            outputFile = file("ember-api/build.gradle.kts")
            patchFile = file("ember-api/build.gradle.kts.patch")
        }
        patchFile {
            path = "paper-checkstyle/build.gradle.kts"
            outputFile = file("ember-checkstyle/build.gradle.kts")
            patchFile = file("ember-checkstyle/build.gradle.kts.patch")
        }
        patchDir("paperApi") {
            upstreamPath = "paper-api"
            excludes = setOf("build.gradle.kts")
            patchesDir = file("ember-api/paper-patches")
            outputDir = file("paper-api")
        }
        patchDir("paperCheckstyle") {
            upstreamPath = "paper-checkstyle"
            excludes = setOf("build.gradle.kts")
            patchesDir = file("ember-checkstyle/paper-patches")
            outputDir = file("paper-checkstyle")
        }
        patchDir("paperCheckstyleConfig") {
            upstreamPath = ".checkstyle"
            patchesDir = file("ember-checkstyle/config-patches")
            outputDir = file(".checkstyle")
        }
    }
}

subprojects {
    apply {
        plugin("java-library")
        plugin("maven-publish")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    // Same set Paper itself skips checkstyle on.
    val checkstyleSkipped = setOf("ember-server", "paper-server", "test-plugin")

    if (name !in checkstyleSkipped) {
        apply { plugin("io.papermc.paperweight.paper-checkstyle") }
        extensions.configure<PaperCheckstyleExt> {
            typeUseAnnotationsFile.set(rootProject.layout.projectDirectory.file(".checkstyle/type_use_annotations.txt"))
        }
        dependencies {
            "checkstyle"(project(":ember-checkstyle"))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
        options.isFork = true
        options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
    }
    tasks.withType<Javadoc>().configureEach {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = Charsets.UTF_8.name()
    }
    tasks.withType<Test>().configureEach {
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
    }
}

tasks.register("printMinecraftVersion") {
    val mcVersion = providers.gradleProperty("mcVersion")
    doLast {
        println(mcVersion.get().trim())
    }
}

tasks.register("printEmberVersion") {
    val v = provider { project.version }
    doLast {
        println(v.get())
    }
}
