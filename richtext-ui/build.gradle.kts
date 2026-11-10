plugins {
  id("richtext-kmp-library")
  id("org.jetbrains.compose") version Compose.desktopVersion
  id("org.jetbrains.kotlin.plugin.compose") version Kotlin.version
  id("org.jetbrains.dokka")
}

repositories {
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

android {
  namespace = "com.halilibo.richtext.ui"
  testOptions.unitTests.isIncludeAndroidResources = true
  testOptions.unitTests.all {
    it.maxHeapSize = "1g"
    it.maxParallelForks = 1
  }
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
      }
    }
    val commonTest by getting

    val androidMain by getting {
      kotlin.srcDir("src/commonJvmAndroid/kotlin")
    }
    val androidUnitTest by getting {
      dependencies {
        implementation("androidx.compose.ui:ui-test-junit4:1.8.2")
        implementation("androidx.compose.ui:ui-test-manifest:1.8.2")
        implementation("junit:junit:4.13.2")
        implementation("org.robolectric:robolectric:4.17")
      }
    }
    val jvmMain by getting {
      kotlin.srcDir("src/commonJvmAndroid/kotlin")
    }
  }
}
