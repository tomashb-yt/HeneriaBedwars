import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("com.diffplug.spotless") version "8.8.0" apply false
    id("com.gradleup.shadow") version "8.3.11" apply false
}

allprojects {
    group = "fr.heneria.bedwars"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.isFork = true
        options.isIncremental = false
        options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    extensions.configure<SpotlessExtension> {
        java {
            googleJavaFormat("1.28.0")
            formatAnnotations()
            removeUnusedImports()
            target("src/**/*.java")
        }
    }
}
