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
    // rx
    implementation(libs.rxJava2)

    // Unit tests
    testImplementation(libs.junit)
}
