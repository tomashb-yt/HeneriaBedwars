description = "Stable public contracts for HeneriaZombie addons."

tasks.jar {
  destinationDirectory.set(
    gradle.gradleUserHomeDir.resolve("heneriazombie/${project.version}/zombie-api"),
  )
}
