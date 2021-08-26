import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    groovy
    application
}

group = "com.github.daedalus"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    //Projects Dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.2")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("org.slf4j:slf4j-log4j12:1.7.25")
    implementation("io.github.microutils:kotlin-logging:1.12.5")
    implementation("com.google.code.gson:gson:2.8.8")

    implementation("org.codehaus.groovy:groovy:3.0.8")
    testImplementation("org.spockframework:spock-bom:2.0-groovy-3.0")
    testImplementation("org.spockframework:spock-core")
    testImplementation("org.hamcrest:hamcrest-core:2.2")
    testImplementation("com.athaydes:spock-reports:2.0-groovy-3.0")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

//Set the executable class
val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }
}

application {
    mainClass.set("MainKt")
}