# ── LiteRT-LM native bridge (JNI symbols must survive) ───────────────────────
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class com.google.ai.edge.litert.** { *; }

# ── Ktor server (reflection-heavy) ───────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn  io.ktor.**
-dontwarn  io.netty.**
-dontwarn  org.slf4j.**

# ── Kotlin serialization ──────────────────────────────────────────────────────
-keep class kotlinx.serialization.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# ── App data/model layer (used via reflection by serialization) ───────────────
-keep class com.laiserdev.localllm.data.model.** { *; }

# ── Enum members (required for all enums) ────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Coroutines ────────────────────────────────────────────────────────────────
-keepclassmembers class kotlinx.** { volatile <fields>; }
