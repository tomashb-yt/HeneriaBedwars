description = "Paper-independent domain and application foundations."

dependencies {
  api(files(rootProject.project(":zombie-api").tasks.named("jar")))
  testImplementation(files(tasks.named("jar")))
}

tasks.jar {
  destinationDirectory.set(
    gradle.gradleUserHomeDir.resolve("heneriazombie/${project.version}/zombie-core"),
  )
}
