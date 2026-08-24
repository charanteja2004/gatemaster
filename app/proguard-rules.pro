# Kotlinx serialization keeps generated serializers via annotations.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Model classes are serialized by name.
-keep,allowobfuscation,allowshrinking class com.gatemaster.app.core.model.** { *; }

# Keep line numbers for readable crash reports, but hide the source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
