# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep the AccessibilityService class
-keep class com.example.qqautotask.QQAutoService { *; }

# Keep the MainActivity class
-keep class com.example.qqautotask.MainActivity { *; }

# Keep all classes in the com.example.qqautotask package
-keep class com.example.qqautotask.** { *; }

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Material
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }

# Keep R classes
-keep class **.R$* { *; }

# Keep annotations
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
