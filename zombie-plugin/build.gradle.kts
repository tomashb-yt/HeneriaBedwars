plugins {
  id("com.gradleup.shadow")
  id("xyz.jpenilla.run-paper")
}

description = "Paper adapter and deployable HeneriaZombie plugin."

dependencies {
  implementation(files(rootProject.project(":zombie-api").tasks.named("jar")))
  implementation(files(rootProject.project(":zombie-core").tasks.named("jar")))

  compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
  testImplementation(files(tasks.named("jar")))
  testImplementation("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
}

tasks.jar {
  archiveClassifier.set("unshaded")
  destinationDirectory.set(
    gradle.gradleUserHomeDir.resolve("heneriazombie/${project.version}/zombie-plugin"),
  )
}

tasks.processResources {
  filesMatching("plugin.yml") {
    expand("version" to project.version)
  }
}

tasks.shadowJar {
  archiveBaseName.set("HeneriaZombie")
  archiveClassifier.set("")
  destinationDirectory.set(
    gradle.gradleUserHomeDir.resolve("heneriazombie/${project.version}/deployable"),
  )
  mergeServiceFiles()
}

val copyDeployableJar by tasks.registering(Copy::class) {
  dependsOn(tasks.shadowJar)
  from(tasks.shadowJar.flatMap { it.archiveFile })
  into(layout.buildDirectory.dir("libs"))
}

tasks.build {
  dependsOn(copyDeployableJar)
}

tasks.runServer {
  minecraftVersion("1.21.11")
  runDirectory(
    gradle.gradleUserHomeDir.resolve("heneriazombie/${project.version}/paper-run"),
  )
  systemProperty("com.mojang.eula.agree", "true")
  pluginJars.from(tasks.shadowJar.flatMap { it.archiveFile })
}

