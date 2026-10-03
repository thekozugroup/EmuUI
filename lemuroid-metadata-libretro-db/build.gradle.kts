plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

val prepareLibretroMetadata =
    tasks.register<PrepareLibretroMetadata>("prepareLibretroMetadata") {
        archiveFiles.from(fileTree("src/main/metadata") { include("libretro-db.sqlite.gz.part-*") })
        expectedArchiveSha256.set("4811f938c06cbbdd49ca7c5ee60cee2f0e4b7359802b3cf52cc78af1adec680c")
        expectedSize.set(12_775_424L)
        expectedSha256.set("4c724302254bef897b248c0e538b8039a731e5e1dc7c0bdc6c255b9407b196d1")
        outputDirectory.set(layout.buildDirectory.dir("generated/libretroMetadata"))
    }

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(prepareLibretroMetadata) { it.outputDirectory }
    }
}

dependencies {
    implementation(project(":retrograde-util"))
    implementation(project(":retrograde-app-shared"))

    implementation(deps.libs.androidx.room.runtime)
    implementation(deps.libs.androidx.room.ktx)
    implementation(deps.libs.dagger.core)
    implementation(deps.libs.kotlinxCoroutinesAndroid)

    kapt(deps.libs.androidx.room.compiler)
    kapt(deps.libs.dagger.compiler)
}

android {
    resourcePrefix("libretrodb_")
    kotlinOptions {
        jvmTarget = "17"
    }
    namespace = "com.swordfish.lemuroid.metadata.libretrodb"
}
