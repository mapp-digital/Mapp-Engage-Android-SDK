# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

###############################################
# General ProGuard rules for Android libraries
###############################################

# Keep all public/protected members in the SDK's own packages only.
# Using "public class *" is too broad — it would prevent obfuscation of every
# transitive dependency. Narrowing to com.appoxee.** is sufficient.
-keep class com.appoxee.** {
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

-dontwarn java.lang.invoke.StringConcatFactory
