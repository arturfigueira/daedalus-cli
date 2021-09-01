import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    groovy
    application
    jacoco
    id("org.sonarqube") version "3.3"
}

group = "com.github.daedalus"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

sonarqube {
    properties {
        property("sonar.projectKey", "arturfigueira_daedalus-cli")
        property("sonar.organization", "arturfigueira")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

jacoco {
    toolVersion = "0.8.7"
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

    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.13.1")
    implementation("com.sksamuel.hoplite:hoplite-core:1.4.7")
    implementation("com.sksamuel.hoplite:hoplite-yaml:1.4.7")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.4")
    implementation("com.google.guava:guava:30.1.1-jre")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

tasks.test{
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(false)
    }// tests are required to run before generating the report
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