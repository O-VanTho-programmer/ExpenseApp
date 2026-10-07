# ProGuard rules for ExpenseTracker
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
