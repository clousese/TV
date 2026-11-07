# TV
-keep class androidx.leanback.widget.** { *; }

-keep class com.fongmi.android.tv.bean.** { *; }

-keep class androidx.core.content.FileProvider {
  public static android.net.Uri getUriForFile(android.content.Context, java.lang.String, java.io.File);
}

# Gson
-keep class com.google.gson.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okio.** { *; }
-keep class okhttp3.** { *; }

# SLF4J
-keep class org.slf4j.** { *; }

# Kotlin
-keep class kotlin.** { *; }

# Nano
-keep class fi.iki.elonen.** { *; }

# TVBus
-keep class com.tvbus.engine.** { *; }

