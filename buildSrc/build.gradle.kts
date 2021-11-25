plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("wmmp") {
            id = "com.rainyseason.wmmp"
            implementationClass = "com.rainyseason.wmmp.WorkManagerMinPeriodicPlugin"
        }
    }
}

val kotlinVersion = "1.5.30"
val androidGradlePluginVersion = "7.0.2"

dependencies {
    implementation("com.android.tools.build:gradle:$androidGradlePluginVersion")
    implementation("com.android.tools.build:gradle-api:$androidGradlePluginVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.javassist:javassist:3.28.0-GA")
    implementation("commons-io:commons-io:2.11.0")


}
