plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.webscraper"
version = "0.0.1-SNAPSHOT"


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-webflux:6.2.1")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0")
    implementation("org.springframework.retry:spring-retry:2.0.11")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.2")
    implementation ("com.github.crawler-commons:crawler-commons:1.4")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.jsoup:jsoup:1.18.3")

}

tasks.withType<Test> {
    useJUnitPlatform()
}
