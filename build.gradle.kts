import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionWithCurrent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.inputStream

application {
    mainClass.set("com.github.ekenstein.gib2sgf.MainKt")
}

plugins {
    kotlin("jvm") version "1.6.21"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("com.github.ben-manes.versions") version "0.42.0"
    application
}

group = "com.github.ekenstein"
version = "0.1.1"
val kotlinJvmTarget = "1.8"
val junitVersion by extra("5.8.2")
val kotlinVersion by extra("1.6.20")

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx", "kotlinx-cli", "0.3.4")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter", "junit-jupiter-params", junitVersion)
    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    implementation("com.github.Ekenstein", "gibson", "0.1.4")
    implementation("com.github.Ekenstein", "haengma", "2.2.0")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
}

tasks {
    dependencyUpdates {
        rejectVersionIf(UpgradeToUnstableFilter())
    }

    register<Copy>("packageDistribution") {
        dependsOn("jar")
        from("${project.rootDir}/scripts/gib2sgf")

        from("${project.projectDir}/build/libs/${project.name}.jar") {
            into("lib")
        }

        into("${project.rootDir}/dist")
    }

    val dependencyUpdateSentinel = register<DependencyUpdateSentinel>("dependencyUpdateSentinel", buildDir)
    dependencyUpdateSentinel.configure {
        dependsOn(dependencyUpdates)
    }

    withType<KotlinCompile>() {
        kotlinOptions.jvmTarget = "1.8"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = kotlinJvmTarget
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = kotlinJvmTarget
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    compileJava {
        sourceCompatibility = kotlinJvmTarget
        targetCompatibility = kotlinJvmTarget
    }

    compileTestJava {
        sourceCompatibility = kotlinJvmTarget
        targetCompatibility = kotlinJvmTarget
    }

    check {
        dependsOn(test)
        dependsOn(ktlintCheck)
        dependsOn(dependencyUpdateSentinel)
    }

    test {
        useJUnitPlatform()
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes["Main-Class"] = application.mainClass
        }

        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        archiveFileName.set("${project.name}.jar")
    }
}

ktlint {
    version.set("0.45.2")
}

class UpgradeToUnstableFilter : com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentFilter {
    override fun reject(cs: ComponentSelectionWithCurrent) = reject(cs.currentVersion, cs.candidate.version)

    private fun reject(old: String, new: String): Boolean {
        return !isStable(new) && isStable(old) // no unstable proposals for stable dependencies
    }

    private fun isStable(version: String): Boolean {
        val stableKeyword = setOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        val stablePattern = version.matches(Regex("""^[0-9,.v-]+(-r)?$"""))
        return stableKeyword || stablePattern
    }
}

abstract class DependencyUpdateSentinel @Inject constructor(private val buildDir: File) : DefaultTask() {
    @ExperimentalPathApi
    @org.gradle.api.tasks.TaskAction
    fun check() {
        val updateIndicator = "The following dependencies have later milestone versions:"
        val report = Paths.get(buildDir.toString(), "dependencyUpdates", "report.txt")

        report.inputStream().bufferedReader().use { reader ->
            if (reader.lines().anyMatch { it == updateIndicator }) {
                throw GradleException("Dependency updates are available.")
            }
        }
    }
}
