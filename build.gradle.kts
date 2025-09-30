plugins {
    java
    id("me.champeau.jmh") version "0.7.3"
    id("com.gradleup.shadow") version "9.2.0"
}

group = "me.towdium.pinin"
version = "1.6.0"

repositories {
    mavenCentral()
}

tasks.shadowJar {
    minimize()
    relocate("it.unimi.dsi.fastutil", "me.towdium.pinin.fastutil")
}

jmh {
    resultFormat = "JSON"
}

tasks.test {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 8
}

dependencies {
    implementation("it.unimi.dsi:fastutil:8.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.openjdk.jol:jol-core:0.17")
    jmhImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
