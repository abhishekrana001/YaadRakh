# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Room Database Rules
-keep class com.example.yaadrakh.data.local.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
-dontwarn androidx.room.paging.**

# Keep ViewBinding classes
-keep class com.example.yaadrakh.databinding.** { *; }
