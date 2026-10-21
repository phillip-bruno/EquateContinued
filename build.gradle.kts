plugins {
    alias(libs.plugins.android.application) apply false
    // AGP's built-in Kotlin support compiles the .kt sources; declaring the
    // plugin here pins the Kotlin compiler version it uses.
    alias(libs.plugins.kotlin.android) apply false
}
