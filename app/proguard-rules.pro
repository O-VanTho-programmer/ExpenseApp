# ProGuard / R8 rules for ExpenseTracker

# Preserve Annotations & Reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database & Entities
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class * extends androidx.room.RoomDatabase
-keep class com.antigravity.expensetracker.data.local.entity.** { *; }
-keep class com.antigravity.expensetracker.data.model.** { *; }
-dontwarn androidx.room.paging.**

# SQLCipher Native & Java bindings
-keep class net.sqlcipher.** { *; }
-keep interface net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Strip logging statements in release builds to prevent financial data leakage (OWASP M9)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
