# Money Tracker – R8 / ProGuard

# Keep Room entities & DAOs (KSP-generated accessors rely on names)
-keep class com.sumedh.moneytracker.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin / Coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ML Kit barcode (unbundled Play Services model)
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }

# Compose – keep metadata used by runtime
-keep class androidx.compose.runtime.** { *; }

# Enums used via reflection / Parcelable-ish prefs
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# App models stored in SharedPreferences / draft session
-keep class com.sumedh.moneytracker.domain.** { *; }
