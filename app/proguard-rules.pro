# Add project-specific ProGuard rules here.
# By default, the build system uses R8 for code shrinking.

# Keep all data classes (Models)
-keep class com.example.offlinenotes.model.** { *; }

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.util.TableInfo
-keep class androidx.room.DatabaseConfiguration