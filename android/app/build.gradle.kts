import java.util.concurrent.TimeUnit

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.hridaya.kubenexus"
    compileSdk {
        version = release(37)
    }

    ndkVersion = "27.0.12077973"

    val repoDir = project.rootDir.parentFile ?: project.rootDir

    fun getGitCommitSha(path: String? = null): String {
        return try {
            val execOutput = providers.exec {
                workingDir = repoDir
                if (path != null) {
                    commandLine("git", "log", "-n", "1", "--format=%h", "--", path)
                } else {
                    commandLine("git", "rev-parse", "--short", "HEAD")
                }
            }.standardOutput.asText.get().trim()
            if (execOutput.isNotEmpty()) execOutput else "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun extractGhosttyZonSha(): String {
        return try {
            val zonFile = File(repoDir, "terminal-native/build.zig.zon")
            if (zonFile.exists()) {
                val content = zonFile.readText()
                val match = Regex("""github\.com/ghostty-org/ghostty#([a-f0-9]+)""").find(content)
                match?.groupValues?.get(1)?.take(7) ?: "a746d0f"
            } else {
                "a746d0f"
            }
        } catch (e: Exception) {
            "a746d0f"
        }
    }

    val appCommitSha = System.getenv("KUBENEXUS_APP_COMMIT_SHA") ?: getGitCommitSha()
    val libghosttyCommitSha = System.getenv("KUBENEXUS_LIBGHOSTTY_COMMIT_SHA") ?: extractGhosttyZonSha()
    val ghosttyBridgeCommitSha = System.getenv("KUBENEXUS_GHOSTTY_BRIDGE_COMMIT_SHA") ?: getGitCommitSha("terminal-native")
    val goCoreCommitSha = System.getenv("KUBENEXUS_GO_CORE_COMMIT_SHA") ?: getGitCommitSha("core")
    val clientGoCommitSha = System.getenv("KUBENEXUS_CLIENT_GO_COMMIT_SHA") ?: "44a8af2"

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
        buildConfigField("String", "CLIENT_GO_COMMIT_SHA", "\"$clientGoCommitSha\"")

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        create("release") {
            val keystorePath = project.findProperty("KEYSTORE_PATH") as? String
                ?: System.getenv("KEYSTORE_PATH")
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = project.findProperty("KEYSTORE_PASSWORD") as? String
                    ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = project.findProperty("KEY_ALIAS") as? String
                    ?: System.getenv("KEY_ALIAS") ?: ""
                keyPassword = project.findProperty("KEY_PASSWORD") as? String
                    ?: System.getenv("KEY_PASSWORD") ?: ""
            } else {
                initWith(getByName("debug"))
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
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
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

dependencies {
    // kubenexus-sources.jar sits alongside kubenexus.aar so Android Studio can
    // attach Go doc comments and real parameter names to the generated bindings.
    // It must stay off the compile classpath.
    implementation(
        fileTree(
            mapOf(
                "dir" to "libs",
                "include" to listOf("*.jar", "*.aar"),
                "exclude" to listOf("*-sources.jar"),
            ),
        ),
    )

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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

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
