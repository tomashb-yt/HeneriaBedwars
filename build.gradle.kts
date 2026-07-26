import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
  base
  id("com.diffplug.spotless") version "8.8.0"
  id("com.gradleup.shadow") version "8.3.11" apply false
  id("xyz.jpenilla.run-paper") version "3.0.2" apply false
}

group = "fr.heneria.zombie"
version = "0.8.0-SNAPSHOT"

allprojects {
  group = rootProject.group
  version = rootProject.version
}

spotless {
  format("rootMisc") {
    target(
      "*.gradle.kts",
      "*.properties",
      "*.md",
      ".editorconfig",
      ".gitattributes",
      ".gitignore",
      "docs/**/*.md",
    )
    trimTrailingWhitespace()
    endWithNewline()
  }
}

subprojects {
  apply(plugin = "java-library")
  apply(plugin = "jacoco")
  apply(plugin = "com.diffplug.spotless")

  extensions.configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
  }

  dependencies {
    "testImplementation"(platform("org.junit:junit-bom:5.13.4"))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
  }

  tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
    options.isFork = true
    options.isIncremental = false
    options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
  }

  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
      events("failed", "skipped")
      exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
  }

  tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:-missing", true)
    options.encoding = "UTF-8"
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
    format("moduleMisc") {
      target("*.gradle.kts", "src/**/*.yml", "src/**/*.yaml", "src/**/*.json", "src/**/*.md")
      trimTrailingWhitespace()
      endWithNewline()
    }
  }
}

tasks.register("qualityGate") {
  group = "verification"
  description = "Runs formatting, tests, build and the deployable JAR task."
  dependsOn("spotlessCheck", subprojects.map { it.tasks.named("check") })
  dependsOn(":zombie-plugin:copyDeployableJar")
}
