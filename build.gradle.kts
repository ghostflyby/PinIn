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

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    exclude("it/unimi/dsi/fastutil/shorts/**")
    exclude("it/unimi/dsi/fastutil/longs/**")
    exclude("it/unimi/dsi/fastutil/floats/**")
    exclude("it/unimi/dsi/fastutil/doubles/**")
    exclude("it/unimi/dsi/fastutil/bytes/**")
    exclude("it/unimi/dsi/fastutil/booleans/**")
    exclude("it/unimi/dsi/fastutil/ints/*Map*")
    exclude("it/unimi/dsi/fastutil/objects/*2Short*Map*")
    exclude("it/unimi/dsi/fastutil/objects/*2Reference*Map*")
    exclude("it/unimi/dsi/fastutil/objects/*2Long*Map*")
    exclude("it/unimi/dsi/fastutil/objects/*2Int*Map*")
    exclude("it/unimi/dsi/fastutil/objects/*2Float*Map*")
    exclude("it/unimi/dsi/fastutil/objects/*2Double*Map*")
    exclude("it/unimi/dsi/fastutil/objects/*2Char*Map*")
    exclude("it/unimi/dsi/fastutil/objects/*2Byte*Map*")
    exclude("it/unimi/dsi/fastutil/objects/*2Boolean*Map*")
    exclude("it/unimi/dsi/fastutil/chars/*2Short*Map*")
    exclude("it/unimi/dsi/fastutil/chars/*2Reference*Map*")
    exclude("it/unimi/dsi/fastutil/chars/*2Long*Map*")
    exclude("it/unimi/dsi/fastutil/chars/*2Int*Map*")
    exclude("it/unimi/dsi/fastutil/chars/*2Float*Map*")
    exclude("it/unimi/dsi/fastutil/chars/*2Double*Map*")
    exclude("it/unimi/dsi/fastutil/chars/*2Char*Map*")
    exclude("it/unimi/dsi/fastutil/chars/*2Byte*Map*")
    exclude("it/unimi/dsi/fastutil/chars/*2Boolean*Map*")
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
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
