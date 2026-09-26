plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.aboutLibraries)
}

// Set while configuring android.signingConfigs below; null when the upload key is usable.
var releaseSigningMissing: String? = null

android {
    namespace = "dev.hridaya.kubenexus"
    compileSdk {
        version = release(37)
    }

    // Must match ANDROID_NDK_HOME in the Makefile. If it names an NDK that is not installed, AGP
    // ships the native libs unstripped and emits no debug symbols. `make verify-ndk` checks it.
    ndkVersion = "30.0.16248370"

    val repoDir = project.rootDir.parentFile ?: project.rootDir

    fun gitCommitSha(path: String? = null): String? = try {
        providers.exec {
            workingDir = repoDir
            if (path != null) {
                commandLine("git", "log", "-n", "1", "--format=%h", "--", path)
            } else {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }
        }.standardOutput.asText.get().trim().ifEmpty { null }
    } catch (e: Exception) {
        null
    }

    fun pinnedGhosttySha(): String? = File(repoDir, "terminal-native/build.zig.zon")
        .takeIf { it.isFile }
        ?.let { Regex("""github\.com/ghostty-org/ghostty#([a-f0-9]+)""").find(it.readText()) }
        ?.groupValues?.get(1)
        ?.take(7)

    fun pinnedClientGoVersion(): String? = File(repoDir, "k8s-engine/go.mod")
        .takeIf { it.isFile }
        ?.let { Regex("""k8s\.io/client-go v([^\s]+)""").find(it.readText()) }
        ?.groupValues?.get(1)

    fun resolved(what: String, envVar: String, derive: () -> String?): String =
        System.getenv(envVar)?.trim()?.ifEmpty { null }
            ?: derive()
            ?: throw GradleException(
                "Could not determine $what. Build from a git checkout with the submodules present, " +
                    "or set $envVar explicitly."
            )

    val appCommitSha = resolved("the app commit SHA", "KUBENEXUS_APP_COMMIT_SHA") { gitCommitSha() }
    val libghosttyCommitSha = resolved(
        "the pinned libghostty commit",
        "KUBENEXUS_LIBGHOSTTY_COMMIT_SHA",
    ) { pinnedGhosttySha() }
    val ghosttyBridgeCommitSha = resolved(
        "the terminal-native commit SHA",
        "KUBENEXUS_GHOSTTY_BRIDGE_COMMIT_SHA",
    ) { gitCommitSha("terminal-native") }
    val goCoreCommitSha = resolved(
        "the k8s-engine commit SHA",
        "KUBENEXUS_GO_CORE_COMMIT_SHA",
    ) { gitCommitSha("k8s-engine") }
    val clientGoVersion = resolved(
        "the k8s.io/client-go version from k8s-engine/go.mod",
        "KUBENEXUS_CLIENT_GO_VERSION",
    ) { pinnedClientGoVersion() }

    defaultConfig {
        applicationId = "dev.hridaya.kubenexus"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "APP_COMMIT_SHA", "\"$appCommitSha\"")
        buildConfigField("String", "LIBGHOSTTY_COMMIT_SHA", "\"$libghosttyCommitSha\"")
        buildConfigField("String", "GHOSTTY_BRIDGE_COMMIT_SHA", "\"$ghosttyBridgeCommitSha\"")
        buildConfigField("String", "GO_CORE_COMMIT_SHA", "\"$goCoreCommitSha\"")
        buildConfigField("String", "CLIENT_GO_VERSION", "\"$clientGoVersion\"")

        ndk {
            // Ship every ABI the JNI bridges are built for (arm64-v8a,
            // armeabi-v7a, x86_64, x86) so the AAB can be split per device.
            // Removing the filter would also pull in ABIs we do not compile.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    fun signingValue(name: String): String? =
        (project.findProperty(name) as? String ?: System.getenv(name))?.takeIf { it.isNotBlank() }

    val releaseKeystore = signingValue("KEYSTORE_PATH")?.let { file(it) }?.takeIf { it.isFile }
    releaseSigningMissing = when {
        signingValue("KEYSTORE_PATH") == null -> "KEYSTORE_PATH is not set"
        releaseKeystore == null -> "KEYSTORE_PATH does not point to a keystore file"
        else -> listOf("KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
            .filter { signingValue(it) == null }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "missing ")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        // Release builds are signed with the upload key only. There is deliberately no
        // fallback to the debug key: Google Play rejects debug-signed uploads, and a silent
        // fallback turned a missing CI secret into a "release" bundle that could never ship.
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = signingValue("KEYSTORE_PASSWORD")
                keyAlias = signingValue("KEY_ALIAS")
                keyPassword = signingValue("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = false
            }
        }

        release {
            ndk {
                debugSymbolLevel = "FULL"
            }
            isDebuggable = false
            signingConfig = signingConfigs.findByName("release")
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE.txt",
                "/META-INF/LICENSE",
                "/META-INF/NOTICE.txt",
                "/META-INF/NOTICE",
                "/META-INF/*.version",
                "/META-INF/androidx.*",
                "**/*.kotlin_builtins",
                "**/*.kotlin_metadata",
            )
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        disable += listOf(
            "NewerVersionAvailable",
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "ChromeOsAbiSupport",
        )
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

// Refuse to package a release APK or AAB without the upload key, instead of letting AGP
// emit an unsigned artifact. Compiling, linting and unit-testing the release variant keep
// working without it.
val checkReleaseSigning by tasks.registering {
    val problem = releaseSigningMissing
    doLast {
        if (problem != null) {
            throw GradleException(
                "Release signing is not configured ($problem). Set KEYSTORE_PATH, " +
                    "KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD as Gradle properties or " +
                    "environment variables to the upload key.",
            )
        }
    }
}
tasks.matching { it.name == "packageRelease" || it.name == "packageReleaseBundle" }.configureEach {
    dependsOn(checkReleaseSigning)
}

// Gradle cannot see the vendored Zig tree or the Go modules, so android/config/ declares those
// components by hand. Add an entry there when a native dependency changes.
aboutLibraries {
    collect {
        configPath = file("../config")
        fetchRemoteLicense = false
        fetchRemoteFunding = false
    }

    export {
        prettyPrint = true
    }

    license {
        strictMode = com.mikepenz.aboutlibraries.plugin.StrictMode.FAIL

        allowedLicenses.addAll(
            "Apache-2.0",
            "MIT",
            "BSD-2-Clause",
            "BSD-3-Clause",
            "ISC",
            "0BSD",
            "Unlicense",
            "CC0-1.0",
            "OFL-1.1",
            "Zlib",
            "Public Domain",
            "mit-with-copyrights",
            "go-bsd-3-clause",
            "go-module-notices",
            "wuffs-mit",
            "zig-mit",
        )

        // Native components reference these by SPDX id and AboutLibraries pulls the canonical
        // text from spdx.org. MIT and BSD are the exception: those licence bodies carry a
        // "<year> <owner>" placeholder, and both require the project's real copyright notice, so
        // config/licenses/ supplies them with the notices filled in. Only licences a shipped
        // component actually uses are listed, so the licences screen shows no stray texts.
        additionalLicenses.addAll(
            "Apache-2.0",
            "mit-with-copyrights",
            "go-bsd-3-clause",
            "go-module-notices",
            "wuffs-mit",
            "zig-mit",
        )
    }

    library {
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
    }
}

dependencies {
    implementation(
        fileTree(
            mapOf(
                "dir" to "../data/libs",
                "include" to listOf("*.jar", "*.aar"),
                "exclude" to listOf("*-sources.jar"),
            ),
        ),
    )

    // Sub-module dependencies
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Core / Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)

    // Hilt (app still needs @HiltAndroidApp processor)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.leakcanary)
}


