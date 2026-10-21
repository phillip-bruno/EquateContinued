# Project specific rules, appended to proguard-android-optimize.txt.

-dontobfuscate

-optimizations !field/removal/writeonly,!field/marking/private,!class/merging/*,!code/allocation/variable

# Workaround for a release-only crash inside the fragment library.
-keep class androidx.fragment.app.** { *; }
-keepclassmembers class androidx.fragment.app.** { *; }
