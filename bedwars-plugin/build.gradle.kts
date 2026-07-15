plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":bedwars-api"))
    implementation(project(":bedwars-core"))
    compileOnly("org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.2")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT")
    testImplementation("org.jetbrains:annotations:26.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets.main {
    java.srcDir(project(":bedwars-api").file("src/main/java"))
    java.srcDir(project(":bedwars-core").file("src/main/java"))
}

// The local Windows sandbox cannot reliably expose compiled project outputs to test javac.
// Compiling the same sources into the test source set keeps tests hermetic in this workspace.
sourceSets.test {
    java.srcDir("src/main/java")
    java.srcDir(project(":bedwars-api").file("src/main/java"))
    java.srcDir(project(":bedwars-core").file("src/main/java"))
}

tasks.shadowJar {
    archiveBaseName.set("HeneriaBedWars")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
