buildscript {
    dependencies {
        // AGP 9.x provides built-in Kotlin support. We pin KGP explicitly
        // so the Compose compiler and Kotlin toolchain remain deterministic.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
    id("androidx.room") version "2.8.5" apply false
}
