# SourceHub ProGuard Rules

# Keep Room entities
-keep class com.example.sourcehub.data.local.db.entity.** { *; }

# Keep domain models
-keep class com.example.sourcehub.domain.model.** { *; }

# Keep API interfaces
-keep,allowobfuscation interface com.example.sourcehub.data.remote.api.** { *; }

# Keep DTOs
-keep class com.example.sourcehub.data.remote.dto.** { *; }

# Keep security classes
-keep class com.example.sourcehub.security.** { public <methods>; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}
