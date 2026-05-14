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

# Add project specific ProGuard rules here.

# Keep GeckoView classes
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.annotation.WrapForJNI
-keep class org.mozilla.gecko.annotation.RobocopTarget
-keep @org.mozilla.gecko.annotation.WrapForJNI class *
-keep @org.mozilla.gecko.annotation.RobocopTarget class *
-keepclasseswithmembers class * {
    @org.mozilla.gecko.annotation.WrapForJNI *;
}
-keepclasseswithmembers class * {
    @org.mozilla.gecko.annotation.RobocopTarget *;
}

# Keep Room database
-keep class androidx.room.** { *; }
-keep class com.webstudio.easybrowser.database.entity.** { *; }

# Keep model classes
-keep class com.webstudio.easybrowser.models.** { *; }

# Keep Gson specific classes
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.** { *; }

# Keep Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# General Android rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod

# Enum suport
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable support
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# Keep R class
-keepclassmembers class **.R$* {
    public static <fields>;
}

# RecyclerView adapter methods
-keepclassmembers class androidx.recyclerview.widget.RecyclerView$Adapter {
    public <methods>;
}

# Keep custom views
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}