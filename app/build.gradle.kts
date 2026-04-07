import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.tasks.compile.JavaCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.errorprone)
    checkstyle
    pmd
}

android {
    namespace = "xyz.gaon.typoon"

    compileSdk {
        version =
            release(36) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = "xyz.gaon.typoon"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

ktlint {
    android.set(true)
    version.set("1.5.0")
    reporters {
        reporter(ReporterType.PLAIN)
    }
    filter {
        exclude { element -> element.file.path.contains("build/") }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/detekt.yml")
    source.setFrom("src/main/java")
}

checkstyle {
    toolVersion = "10.21.4"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isShowViolations = true
}

pmd {
    toolVersion = "7.18.0"
    ruleSetFiles = files("$rootDir/config/pmd/pmd-ruleset.xml")
    ruleSets = emptyList()
}

spotbugs {
    effort.set(Effort.MAX)
    reportLevel.set(Confidence.LOW)
    showStackTraces.set(true)
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        allErrorsAsWarnings.set(false)
        excludedPaths.set(".*/build/generated/.*")
    }
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

tasks.withType<Pmd>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

val checkstyleMainJava by tasks.registering(Checkstyle::class) {
    description = "Runs Checkstyle on main Java sources."
    group = "verification"
    source("src/main/java")
    include("**/*.java")
    classpath = files()
}

val pmdMainJava by tasks.registering(Pmd::class) {
    description = "Runs PMD on main Java sources."
    group = "verification"
    source("src/main/java")
    include("**/*.java")
    ruleSetFiles = files("$rootDir/config/pmd/pmd-ruleset.xml")
    ruleSets = emptyList()
}

val spotbugsDebug by tasks.registering(SpotBugsTask::class) {
    description = "Runs SpotBugs on debug Java bytecode."
    group = "verification"
    dependsOn("compileDebugKotlin", "compileDebugJavaWithJavac")
    classDirs.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { include("**/*.class") },
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
            include("**/*.class")
        },
    )
    sourceDirs.setFrom(files("src/main/java"))
    auxClassPaths.setFrom(configurations.named("debugCompileClasspath"))
    excludeFilter.set(rootProject.layout.projectDirectory.file("config/spotbugs/exclude.xml"))
    onlyIf { classDirs.files.any { it.exists() } }
}

tasks.register("qualityGate") {
    group = "verification"
    description = "Runs the full static analysis pipeline."
    dependsOn(
        "ktlintCheck",
        "detekt",
        checkstyleMainJava,
        pmdMainJava,
        spotbugsDebug,
        "compileDebugJavaWithJavac",
    )
}

tasks.named("check").configure {
    dependsOn("qualityGate")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)
    implementation(libs.play.app.update)
    implementation(libs.play.services.ads)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    errorprone(libs.errorprone.core)
}
