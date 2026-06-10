plugins {
  id("com.android.library")
  kotlin("multiplatform")
  id("maven-publish")
  id("signing")
}

repositories {
  google()
  mavenCentral()
}

kotlin {
  jvm()
  androidTarget {
    publishLibraryVariants("release")
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
  }
  explicitApi()
}

android {
  compileSdk = AndroidConfiguration.compileSdk
  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  defaultConfig {
    minSdk = AndroidConfiguration.minSdk
    targetSdk = AndroidConfiguration.targetSdk
  }
}
