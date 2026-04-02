# Typoon MVP ProGuard Rules

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# DataStore / Kotlin Serialization
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ConversionEngine (Refraction protection)
-keep class xyz.gaon.typoon.core.engine.** { *; }