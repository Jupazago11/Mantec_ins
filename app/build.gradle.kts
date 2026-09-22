plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.mantec_ins"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mantec_ins"
        minSdk = 24
        targetSdk = 35
        versionCode = 26
        versionName = "1.9.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sin esto el release queda sin firmar (apksigner: "DOES NOT
            // VERIFY") y no se puede instalar en ningun dispositivo, ni
            // para pruebas. No hay todavia una keystore de release real
            // en el proyecto — se firma con la de debug (auto-generada
            // por Android) solo para poder instalar y probar contra
            // produccion. Antes de publicar de verdad (Play Store/
            // distribucion fuera del equipo) esto debe reemplazarse por
            // una keystore de release propia.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "Mantec_v${variant.versionName}_${variant.buildType.name}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    testOptions {
        // Los unit tests JVM (app/src/test) no tienen el framework de
        // Android real — sin esto, cualquier llamada a android.util.Log.*
        // (usada en todos los repos/workers para logging) lanza
        // RuntimeException en vez de ser un no-op.
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    // Offline Supervisor: exporta el esquema de Room a JSON versionado por
    // build, necesario para escribir migraciones explicitas con confianza
    // (Migration(19,20) etc.) en vez de fallbackToDestructiveMigration en
    // produccion — ver PATRONES_ASINCRONISMO_OFFLINE.md patron 5.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")



    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.gson)

    // Miniaturas reales de evidencia ya sincronizada (Supervisor, ver
    // OFFLINE_SUPERVISOR.md) — antes solo icono generico.
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}