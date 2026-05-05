plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":capture-core"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
