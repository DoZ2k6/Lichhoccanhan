plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.lichhoccanhan"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.lichhoccanhan"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/LOOKUP-LOG4J2"

            pickFirsts += "org/apache/pdfbox/resources/**"
            pickFirsts += "org/apache/fontbox/resources/**"
        }
    }
}

dependencies {
    // Đọc PDF
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Đọc và ghi Excel (.xlsx, .xls)
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
        exclude(group = "stax", module = "stax-api")
        exclude(group = "xml-apis", module = "xml-apis")
    }
    implementation("com.fasterxml.woodstox:woodstox-core:6.5.1")

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}