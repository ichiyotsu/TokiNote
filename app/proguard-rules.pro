# Compose and Haze publish their own consumer rules. Keep only the metadata
# needed by Kotlin reflection-free runtime and silence optional annotations.
-keepattributes *Annotation*
-keepattributes Signature,InnerClasses,EnclosingMethod
-dontwarn org.jetbrains.annotations.**
-dontwarn org.intellij.lang.annotations.**
