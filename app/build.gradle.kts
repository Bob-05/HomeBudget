plugins {
    id("com.android.application")
}

android {
    namespace = "com.homebudget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.homebudget"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // OkHttp для сетевых запросов (если нужен)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
// OkHttp для HTTP запросов
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
// Gson для JSON
    implementation("com.google.code.gson:gson:2.10.1")
// Guava для ListenableFuture (если нужно)
    implementation("com.google.guava:guava:32.1.3-android")

    // для форматирования текста
    implementation("io.noties.markwon:core:4.6.0")

    implementation("androidx.work:work-runtime:2.9.0")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // PDF
    implementation("com.itextpdf:itext7-core:7.2.5")

    // Для работы с датами в PDF
    implementation("com.itextpdf:html2pdf:4.0.5")

    //
    implementation("androidx.preference:preference:1.2.1")
}