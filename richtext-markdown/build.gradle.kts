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
  namespace = "com.halilibo.richtext.markdown"
  testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
        api(project(":richtext-ui"))
      }
    }
    val commonTest by getting

    val androidMain by getting {
      dependencies {
        implementation(Compose.coil)
        implementation(Compose.coilNetwork)
        implementation(RaTeX.android)
      }
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
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(Network.okHttp)
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(Kotlin.Test.jdk)
      }
    }
  }
}
