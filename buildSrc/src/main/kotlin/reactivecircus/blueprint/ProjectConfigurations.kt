package reactivecircus.blueprint

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.TestedExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Configure root project.
 * Note that classpath dependencies still need to be defined in the `buildscript` block in the top-level build.gradle.kts file.
 */
fun Project.configureRootProject() {
    // register task for cleaning the build directory in the root project
    tasks.register("clean", Delete::class.java) {
        delete(rootProject.buildDir)
    }

    // configure dokka
    configureDokka()

    // configure binary compatibility validation (API checks)
    configureBinaryCompatibilityValidation()
}

/**
 * Apply baseline project configurations for an Android Library project.
 */
internal fun Project.configureAndroidLibrary() {
    // common android configs
    extensions.getByType<TestedExtension>().configureCommonAndroidOptions()

    // android variant configs
    extensions.getByType<LibraryAndroidComponentsExtension>()
        .configureAndroidLibraryVariants(project)

    // disable unit tests for some build variants if `slimTests` project property is provided
    project.configureSlimTests()
}

/**
 * Apply baseline project configurations for an Android Application project.
 */
internal fun Project.configureAndroidApplication() {
    // common android configs
    extensions.getByType<TestedExtension>().configureCommonAndroidOptions()

    // android variant configs
    extensions.getByType<ApplicationAndroidComponentsExtension>()
        .configureAndroidApplicationVariants(project)

    // disable unit tests for some build variants if `slimTests` project property is provided
    project.configureSlimTests()
}

/**
 * Apply common configurations for all Android projects (Application and Library).
 */
private fun TestedExtension.configureCommonAndroidOptions() {
    setCompileSdkVersion(androidSdk.compileSdk)
    buildToolsVersion(androidSdk.buildTools)

    defaultConfig {
        minSdk = androidSdk.minSdk
        targetSdk = androidSdk.targetSdk

        // only support English for now
        resourceConfigurations.add("en")
    }

    testOptions.animationsDisabled = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

/**
 * Configure the Application Library Component based on build variants.
 */
@Suppress("UnstableApiUsage")
private fun LibraryAndroidComponentsExtension.configureAndroidLibraryVariants(project: Project) {
    project.plugins.withType<KotlinAndroidPluginWrapper> {
        // disable unit test tasks if the unitTest source set is empty
        if (!project.hasUnitTestSource) {
            beforeVariants { it.enableUnitTest = false }
        }

        // disable android test tasks if the androidTest source set is empty
        if (!project.hasAndroidTestSource) {
            beforeVariants { it.enableAndroidTest = false }
        }
    }
}

/**
 * Configure the Application Android Component based on build variants.
 */
@Suppress("UnstableApiUsage")
private fun ApplicationAndroidComponentsExtension.configureAndroidApplicationVariants(project: Project) {
    project.plugins.withType<KotlinAndroidPluginWrapper> {
        // disable unit test tasks if the unitTest source set is empty
        if (!project.hasUnitTestSource) {
            beforeVariants { it.enableUnitTest = false }
        }

        // disable android test tasks if the androidTest source set is empty
        if (!project.hasAndroidTestSource) {
            beforeVariants { it.enableAndroidTest = false }
        }
    }
}

/**
 * Apply common configurations for all projects (including the root project).
 */
@ExperimentalStdlibApi
fun Project.configureForAllProjects(enableExplicitApi: Property<Boolean>) {
    // apply and configure detekt plugin
    configureDetektPlugin()

    // configure maven publishing plugin if applied to the project
    configureMavenPublishing()

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
            freeCompilerArgs = freeCompilerArgs + buildList {
                addAll(additionalCompilerArgs)
                if (enableExplicitApi.get() && !name.contains("test", ignoreCase = true)) {
                    add("-Xexplicit-api=strict")
                }
            }
        }
    }

    tasks.withType<Test>().configureEach {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
}

private val Project.hasUnitTestSource: Boolean
    get() {
        extensions.findByType(KotlinAndroidProjectExtension::class.java)?.sourceSets?.findByName("test")
            ?.let {
                if (it.kotlin.files.isNotEmpty()) return true
            }
        extensions.findByType(KotlinProjectExtension::class.java)?.sourceSets?.findByName("test")
            ?.let {
                if (it.kotlin.files.isNotEmpty()) return true
            }
        return false
    }

private val Project.hasAndroidTestSource: Boolean
    get() {
        extensions.findByType(KotlinAndroidProjectExtension::class.java)?.sourceSets?.findByName("androidTest")
            ?.let {
                if (it.kotlin.files.isNotEmpty()) return true
            }
        return false
    }
