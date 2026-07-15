plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":bedwars-api"))
    implementation(project(":bedwars-core"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets.main {
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
