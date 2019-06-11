import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.21"
}

group = "com.uadaf"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}
val gsonVersion = "2.8.5"
val ktorVersion = "1.2.1"
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}