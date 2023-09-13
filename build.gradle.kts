import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.22"
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("plugin.spring") version "1.8.22"
    kotlin("plugin.jpa") version "1.8.22"
}

group   = "com.midas"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    //implementation("com.zaxxer:HikariCP:5.0.1")
    runtimeOnly("mysql:mysql-connector-java:8.0.33") /* At the time of this writing I can't figure out why the version number is
    needed here but not in some other projects. */
    //implementation("org.hibernate:hibernate-core:5.6.14.Final") /* Is this necessary? **/
    //implementation("org.springframework:spring-orm:5.3.25")

    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("joda-time:joda-time:2.10.9")

    //testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    implementation("org.jetbrains.kotlin:kotlin-reflect")
   // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

/*
kotlin {
    jvmToolchain(11)
}*/