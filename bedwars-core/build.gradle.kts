dependencies {
    api(project(":bedwars-api"))

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// The managed Windows workspace can hide project outputs from javac. Core still has a declared
// API dependency; compiling its sources together is only the same hermetic workaround used by the
// final plugin module.
sourceSets.main {
    java.srcDir(project(":bedwars-api").file("src/main/java"))
}

sourceSets.test {
    java.srcDir("src/main/java")
    java.srcDir(project(":bedwars-api").file("src/main/java"))
}
