plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.jonathan.chatsimpapp"
    compileSdk = 36

    defaultConfig {
        // Se mantiene el mismo applicationId para seguir usando el proyecto de Firebase existente.
        applicationId = "com.jonathan.chatsimpapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // RevenueCat: clave pública de Android ("goog_..."), en ~/.gradle/gradle.properties como revenueCatApiKey.
        buildConfigField("String", "REVENUECAT_API_KEY", "\"${providers.gradleProperty("revenueCatApiKey").getOrElse("")}\"")
        // Servidor en Cloudflare (Worker chat-simpapp-api).
        buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("apiBaseUrl").getOrElse("https://chat-simpapp-api.rj2techsolutions.workers.dev")}\"")
    }

    signingConfigs {
        // Clave debug compartida del proyecto (no es secreta): así la huella SHA-1 registrada en Firebase
        // sirve en cualquier computador. Es la de Android por defecto: contraseña "android".
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Clave de SUBIDA a Google Play: secreta, fuera del repo. Se lee de ~/.gradle/gradle.properties.
        val uploadStore = providers.gradleProperty("uploadStoreFile").orNull
        if (uploadStore != null) {
            create("upload") {
                storeFile = file(uploadStore)
                storePassword = providers.gradleProperty("uploadStorePassword").get()
                keyAlias = providers.gradleProperty("uploadKeyAlias").getOrElse("upload")
                keyPassword = providers.gradleProperty("uploadKeyPassword").get()
            }
        }
    }

    // AdMob. Los IDs no son secretos. Release usa los reales de Chat SimpApp (se pueden cambiar en
    // ~/.gradle/gradle.properties); debug usa SIEMPRE los de prueba de Google, para no generar
    // clics inválidos en la cuenta de AdMob mientras se prueba.
    val admobTest = mapOf(
        "admobAppId" to "ca-app-pub-3940256099942544~3347511713",
        "admobBannerId" to "ca-app-pub-3940256099942544/9214589741",
        "admobAppOpenId" to "ca-app-pub-3940256099942544/9257395921",
        "admobInterstitialId" to "ca-app-pub-3940256099942544/1033173712",
        "admobRewardedId" to "ca-app-pub-3940256099942544/5224354917",
    )
    val admobProd = mapOf(
        "admobAppId" to "ca-app-pub-2631068461383213~6764034990",
        "admobBannerId" to "ca-app-pub-2631068461383213/6847795470",
        "admobAppOpenId" to "ca-app-pub-2631068461383213/5770729550",
        "admobInterstitialId" to "ca-app-pub-2631068461383213/4457647885",
        "admobRewardedId" to "ca-app-pub-2631068461383213/6884500675",
    ).mapValues { (key, value) -> providers.gradleProperty(key).getOrElse(value) }
    fun com.android.build.api.dsl.ApplicationBuildType.admob(ids: Map<String, String>) {
        manifestPlaceholders["admobAppId"] = ids.getValue("admobAppId")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${ids.getValue("admobBannerId")}\"")
        buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"${ids.getValue("admobAppOpenId")}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${ids.getValue("admobInterstitialId")}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${ids.getValue("admobRewardedId")}\"")
    }

    buildTypes {
        debug {
            admob(admobTest)
        }
        release {
            admob(admobProd)
            signingConfigs.findByName("upload")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            // Opcional: espejo de Maven para que Robolectric descargue su runtime (-ProbolectricRepo=...).
            providers.gradleProperty("robolectricRepo").orNull?.let {
                test.systemProperty("robolectric.dependency.repo.url", it)
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.revenuecat)
    implementation(libs.play.services.location)
    implementation(libs.maplibre)
    implementation(libs.geofire)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
