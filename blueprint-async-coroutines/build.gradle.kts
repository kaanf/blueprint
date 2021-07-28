plugins {
    `blueprint-plugin`
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

blueprint {
    enableExplicitApi.set(true)
}

dependencies {
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
