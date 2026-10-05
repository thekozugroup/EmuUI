import java.security.MessageDigest
import com.android.build.gradle.BaseExtension

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(deps.plugins.android)
        classpath(deps.plugins.navigationSafeArgs)
        classpath(deps.plugins.kotlinGradlePlugin)
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version deps.versions.kotlin
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("com.android.test") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version deps.versions.kotlin apply false
    id("androidx.baselineprofile") version "1.2.4" apply false
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version deps.versions.kotlin apply false
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }

    configurations.all {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "com.google.android.gms" -> useVersion(deps.versions.gms)
                "org.jetbrains.kotlin" -> {
                    if (requested.name.startsWith("kotlin-stdlib-jre")) {
                        with(requested) {
                            useTarget("$group:${name.replace("jre", "jdk")}:$version")
                        }
                    }
                    useVersion(deps.versions.kotlin)
                }
            }
        }
    }
}

subprojects {
    // Native feature modules still run kapt; keep Kotlin aligned with Java on JDK 21.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
    afterEvaluate {
        if (hasProperty("android")) {
            // BaseExtension is common parent for application, library and test modules
            apply(plugin = "org.jlleitschuh.gradle.ktlint")

            // Source-built replacements are staged separately from the pinned upstream checkout.
            val replacementNames = mapOf(
                "lemuroid_core_mednafen_wswan" to "libmednafen_wswan_libretro_android.so",
                "lemuroid_core_ppsspp" to "libppsspp_libretro_android.so",
                "lemuroid_core_citra" to "libcitra_libretro_android.so",
                "lemuroid_core_fceumm" to "libfceumm_libretro_android.so",
                "lemuroid_core_gambatte" to "libgambatte_libretro_android.so",
                "lemuroid_core_handy" to "libhandy_libretro_android.so",
                "lemuroid_core_mednafen_ngp" to "libmednafen_ngp_libretro_android.so",
                "lemuroid_core_prosystem" to "libprosystem_libretro_android.so",
                "lemuroid_core_stella" to "libstella_libretro_android.so",
                "lemuroid_core_desmume" to "libdesmume_libretro_android.so",
                "lemuroid_core_dosbox_pure" to "libdosbox_pure_libretro_android.so",
                "lemuroid_core_mednafen_pce_fast" to "libmednafen_pce_fast_libretro_android.so",
                "lemuroid_core_mgba" to "libmgba_libretro_android.so",
                "lemuroid_core_mupen64plus_next_gles3" to "libmupen64plus_next_gles3_libretro_android.so",
                "lemuroid_core_pcsx_rearmed" to "libpcsx_rearmed_libretro_android.so",
                "lemuroid_core_melonds" to "libmelonds_libretro_android.so",
            )
            val selectedReplacements = if (project.name == "bundled-cores") {
                replacementNames.values.toSet()
            } else {
                setOfNotNull(replacementNames[project.name])
            }
            if (selectedReplacements.isNotEmpty()) {
                val nativeInput = rootProject.file(".release-native/arm64-v8a")
                val lockFile = rootProject.file("qa/source-core-binaries.json")
                val lockedHashes = groovy.json.JsonSlurper().parse(lockFile) as Map<*, *>
                val originalInput = file("src/main/jniLibs")
                val stagedOutput = layout.buildDirectory.dir("generated/sourceCoreJniLibs")
                val stageSourceCores = tasks.register<Sync>("stageSourceCores") {
                    inputs.file(lockFile)
                    from(originalInput) {
                        selectedReplacements.forEach { exclude("**/$it") }
                    }
                    from(nativeInput) {
                        include(selectedReplacements)
                        into("arm64-v8a")
                    }
                    into(stagedOutput)
                    doFirst {
                        selectedReplacements.forEach { name ->
                            val binary = nativeInput.resolve(name)
                            check(binary.isFile) { "Missing source-built core $name. See docs/SOURCE_CORE_INTEGRATION.md." }
                            val actual = MessageDigest.getInstance("SHA-256")
                                .digest(binary.readBytes()).joinToString("") { "%02x".format(it) }
                            check(actual == lockedHashes[name]) { "Source-built core hash mismatch: $name" }
                        }
                    }
                }
                extensions.configure(BaseExtension::class.java) {
                    sourceSets.getByName("main").jniLibs.setSrcDirs(listOf(stagedOutput))
                }
                tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(stageSourceCores) }
            }

            extensions.configure(BaseExtension::class.java) {
                // Apply to the bundled QA flavor as well as the Play feature-module list.
                // Keep pinned upstream sources intact; omit restricted executable cores.
                if (project.name == "bundled-cores") {
                    packagingOptions {
                        // Library AAR packaging does not apply application ndk.abiFilters.
                        listOf("armeabi-v7a", "x86", "x86_64").forEach { abi ->
                            exclude("lib/$abi/**")
                        }
                        listOf("fbneo", "genesis_plus_gx", "mame2003_plus", "snes9x").forEach {
                            exclude("lib/*/lib${it}_libretro_android.so")
                        }
                    }
                }
                compileSdkVersion(deps.android.compileSdkVersion)
                buildToolsVersion(deps.android.buildToolsVersion)
                defaultConfig {
                    // Approved first-release device scope; identical across base/features/QA.
                    ndk {
                        abiFilters.clear()
                        abiFilters.add("arm64-v8a")
                    }
                    minSdkVersion(deps.android.minSdkVersion)
                    targetSdkVersion(deps.android.targetSdkVersion)
                    multiDexEnabled = true
                }
                lintOptions {
                    isAbortOnError = true
                    disable("UnusedResources") // https://issuetracker.google.com/issues/63150366
                    disable("InvalidPackage")
                    disable("VectorPath")
                    disable("TrustAllX509TrustManager")
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
        }
    }

    configurations {
        all {
            exclude(group = "com.google.code.findbugs", module = "jsr305")
        }
    }
}

tasks {
    "clean"(Delete::class) {
        delete(buildDir)
    }
}
