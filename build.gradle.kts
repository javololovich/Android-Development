// In root build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
 //   id("com.google.devtools.ksp") version "2.0.0-1.0.19" apply false
}