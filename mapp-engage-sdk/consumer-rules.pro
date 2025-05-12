###############################################
# General ProGuard rules for Android libraries
###############################################

# Keep all public classes, their public methods and fields — this helps preserve API surface
-keep public class * {
    public protected *;
}

###############################################
# Kotlin-specific rules
###############################################

# Keep Kotlin metadata (important for Kotlin reflection and tools)
-keep class kotlin.Metadata { *; }

# Keep Kotlin companion objects
-keepclassmembers class ** {
    companion object;
}

# Keep Kotlin default methods
-keepclassmembers class * {
    @kotlin.jvm.JvmStatic <methods>;
}

# Keep synthetic default constructors created for Kotlin data classes, etc.
-keepclassmembers class * {
    <init>(...);
}

###############################################
# Annotations and Reflection (e.g., Moshi, Gson, Retrofit, Dagger)
###############################################

# Keep classes with custom annotations used by reflection
-keep @interface ** {
    *;
}

# Keep classes that might be accessed via reflection
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.squareup.moshi.Json <fields>;
    @retrofit2.http.* <methods>;
}

###############################################
# Appoxee Shared Package — Keep Entire Package
###############################################

# Keep all classes, methods, and fields in com.appoxee.shared and subpackages
-keep class com.appoxee.shared.** { *; }

-keep class com.appoxee.Appoxee {*;}

-keep class java.lang.** { *; }

-dontwarn java.lang.invoke.StringConcatFactory
